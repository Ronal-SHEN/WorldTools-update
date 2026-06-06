package org.waste.of.time.storage.cache
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.world.level.block.entity.*

import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.vehicle.MinecartHopper
import net.minecraft.world.entity.vehicle.ContainerEntity
import net.minecraft.world.inventory.PlayerEnderChestContainer
import net.minecraft.world.SimpleContainer
import org.waste.of.time.WorldTools.mc
import org.waste.of.time.storage.cache.HotCache.markScanned
import org.waste.of.time.storage.cache.HotCache.scannedBlockEntities

object DataInjectionHandler {
    fun onScreenRemoved(screen: Screen) {
        HotCache.lastInteractedBlockEntity?.let {
            handleBlockEntity(screen, it)
        }
        HotCache.lastInteractedEntity?.let {
            handleEntity(screen, it)
        }
    }

    private fun handleEntity(screen: Screen, entity: Entity) {
        when (screen) {
            is ContainerScreen -> {
                (entity as? ContainerEntity)?.dataToVehicle(screen)
            }
            is HopperScreen -> {
                (entity as? MinecartHopper)?.dataToHopperMinecart(screen)
            }
        }

        entity.markScanned()
    }

    private fun ContainerEntity.dataToVehicle(screen: ContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun MinecartHopper.dataToHopperMinecart(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun handleBlockEntity(screen: Screen, blockEntity: BlockEntity, ) {
        when (screen) {
            is ContainerScreen -> {
                when (blockEntity) {
                    is ChestBlockEntity -> blockEntity.dataToChest(screen)
                    is BarrelBlockEntity -> blockEntity.dataToBarrelBlock(screen)
                    is EnderChestBlockEntity -> dataToEnderChest(screen)
                }
            }

            is DispenserScreen -> {
                (blockEntity as? DispenserBlockEntity)?.dataToDispenserOrDropper(screen)
            }

            is AbstractFurnaceScreen<*> -> {
                (blockEntity as? AbstractFurnaceBlockEntity)?.dataToFurnace(screen)
            }

            is BrewingStandScreen -> {
                (blockEntity as? BrewingStandBlockEntity)?.dataToBrewingStand(screen)
            }

            is HopperScreen -> {
                (blockEntity as? HopperBlockEntity)?.dataToHopper(screen)
            }

            is ShulkerBoxScreen -> {
                (blockEntity as? ShulkerBoxBlockEntity)?.dataToShulkerBox(screen)
            }

            is LecternScreen -> {
                (blockEntity as? LecternBlockEntity)?.dataToLectern(screen)
            }

            is CrafterScreen -> {
                (blockEntity as? CrafterBlockEntity)?.dataToCrafter(screen)
            }
        }

        // ToDo: Add support for entity containers like chest boat and minecart

        // ToDo: Find out if its possible to get the map state update (currently has no effect)
        //        screen.getContainerSlots().filter {
        //            it.stack.item == Items.FILLED_MAP
        //        }.forEach {
        //            it.stack.components.get(DataComponentTypes.MAP_ID)?.let { id ->
        //                HotCache.mapIDs.add(id.id)
        //            }
        //        }

        blockEntity.markScanned()
    }

    private fun dataToEnderChest(screen: ContainerScreen) {
        if (mc.isInSingleplayer) return
        val inventory = screen.screenHandler.inventory as? SimpleContainer ?: return
        if (inventory.size() != 27) return
        mc.player?.enderChestInventory = PlayerEnderChestContainer().apply {
            repeat(inventory.size()) { i ->
                setStack(i, inventory.getStack(i))
            }
        }
    }

    private fun AbstractFurnaceBlockEntity.dataToFurnace(screen: AbstractFurnaceScreen<*>) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BarrelBlockEntity.dataToBarrelBlock(screen: ContainerScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun BrewingStandBlockEntity.dataToBrewingStand(screen: BrewingStandScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ChestBlockEntity.dataToChest(screen: ContainerScreen) {
        val facing = cachedState[ChestBlock.FACING] ?: return
        val chestType = cachedState[ChestBlock.TYPE] ?: return
        val containerSlots = screen.getContainerSlots()
        val inventories = containerSlots.partition { it.index < 27 }

        when (chestType) {
            ChestType.SINGLE -> {
                containerSlots.forEach {
                    setStack(it.index, it.stack)
                }
            }

            ChestType.LEFT -> {
                val pos = pos.offset(facing.rotateYClockwise())
                val otherChest = world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    otherChest.setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    setStack(it.index - 27, it.stack)
                }

                scannedBlockEntities[otherChest.pos] = otherChest
            }

            ChestType.RIGHT -> {
                val pos = pos.offset(facing.rotateYCounterclockwise())
                val otherChest = world?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    setStack(it.index, it.stack)
                }
                inventories.second.forEach {
                    otherChest.setStack(it.index - 27, it.stack)
                }

                scannedBlockEntities[otherChest.pos] = otherChest
            }
        }
    }

    private fun DispenserBlockEntity.dataToDispenserOrDropper(screen: DispenserScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun HopperBlockEntity.dataToHopper(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun ShulkerBoxBlockEntity.dataToShulkerBox(screen: ShulkerBoxScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
        }
    }

    private fun LecternBlockEntity.dataToLectern(screen: LecternScreen) {
        book = screen.screenHandler.bookItem
    }

    private fun CrafterBlockEntity.dataToCrafter(screen: CrafterScreen) {
        screen.getContainerSlots().forEach {
            setStack(it.index, it.stack)
            setSlotEnabled(it.index, !isSlotDisabled(it.index))
        }
    }

    private fun AbstractContainerScreen<*>.getContainerSlots() = screenHandler.slots.filter { it.inventory !is Inventory }
}
