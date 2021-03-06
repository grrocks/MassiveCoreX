package com.massivecraft.massivecore.mixin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.massivecraft.massivecore.engine.EngineMassiveCoreTeleportMixinCause;
import com.massivecraft.massivecore.event.EventMassiveCorePlayerPSTeleport;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.ScheduledTeleport;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;

public class TeleportMixinDefault extends TeleportMixinAbstract
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TeleportMixinDefault i = new TeleportMixinDefault();
	public static TeleportMixinDefault get() { return i; }
	
	// -------------------------------------------- //
	// CORE LOGIC
	// -------------------------------------------- //
	
	public static void teleportPlayer(Player player, PS ps) throws TeleporterException
	{
		// Base the PS location on the entity location
		ps = ps.getEntity(true);
		ps = PS.valueOf(player.getLocation()).with(ps);
		
		// Bukkit Location
		Location location = null;
		try
		{
			location = ps.asBukkitLocation();
			
		}
		catch (Exception e)
		{
			throw new TeleporterException(Txt.parse("<b>Could not calculate the location: %s", e.getMessage()));
		}
		
		// eject passengers and unmount before transport
		player.eject();
		Entity vehicle = player.getVehicle();
		if (vehicle != null) vehicle.eject();
		
		// Do the teleport
		EngineMassiveCoreTeleportMixinCause.get().setMixinCausedTeleportIncoming(true);
		player.teleport(location);
		EngineMassiveCoreTeleportMixinCause.get().setMixinCausedTeleportIncoming(false);
		
		// Bukkit velocity
		Vector velocity = null;
		try
		{
			velocity = ps.asBukkitVelocity();
		}
		catch (Exception e)
		{
			return;
		}
		player.setVelocity(velocity);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void teleport(Object teleporteeObject, Destination destination, int delaySeconds) throws TeleporterException
	{
		String teleporteeId = IdUtil.getId(teleporteeObject);
		if (!IdUtil.isPlayerId(teleporteeId)) throw new TeleporterException(Txt.parse("<white>%s <b>is not a player.", Mixin.getDisplayName(teleporteeId, IdUtil.getConsole())));
		
		PS ps;
		try
		{
			ps = destination.getPs(teleporteeId);
		}
		catch (Exception e)
		{
			throw new TeleporterException(e.getMessage());
		}
		
		String desc = destination.getDesc(teleporteeId);
		if (delaySeconds > 0)
		{
			// With delay
			if (desc != null && ! desc.isEmpty())
			{
				Mixin.msgOne(teleporteeId, "<i>Teleporting to <h>"+desc+" <i>in <h>"+delaySeconds+"s <i>unless you move.");
			}
			else
			{
				Mixin.msgOne(teleporteeId, "<i>Teleporting in <h>"+delaySeconds+"s <i>unless you move.");
			}
			
			new ScheduledTeleport(teleporteeId, destination, delaySeconds).schedule();
		}
		else
		{
			// Without delay AKA "now"/"at once"
			
			// Run event
			EventMassiveCorePlayerPSTeleport event = new EventMassiveCorePlayerPSTeleport(teleporteeId, Mixin.getSenderPs(teleporteeId), destination);
			event.run();
			if (event.isCancelled()) return;
			destination = event.getDestination();
			desc = destination.getDesc(teleporteeId);
			
			if (desc != null && ! desc.isEmpty())
			{
				Mixin.msgOne(teleporteeId, "<i>Teleporting to <h>"+desc+"<i>.");
			}
			
			Player teleportee = IdUtil.getPlayer(teleporteeId);
			if (teleportee != null)
			{
				teleportPlayer(teleportee, ps);
			}
			else
			{
				Mixin.setSenderPs(teleporteeId, ps);
			}
		}
	}
	
}
