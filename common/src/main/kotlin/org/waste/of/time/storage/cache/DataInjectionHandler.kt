package org.waste.of.time.storage.cache
import net.minecraft.client.gui.screens.inventory.*
import net.minecraft.world.level.block.entity.*

import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper
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
            setItem(it.containerSlot, it.item)
        }
    }

    private fun MinecartHopper.dataToHopperMinecart(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
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
        //            it.item.item == Items.FILLED_MAP
        //        }.forEach {
        //            it.item.components.get(DataComponentTypes.MAP_ID)?.let { id ->
        //                HotCache.mapIDs.add(id.id)
        //            }
        //        }

        blockEntity.markScanned()
    }

    private fun dataToEnderChest(screen: ContainerScreen) {
        if (mc.isLocalServer) return
        val inventory = screen.menu.container as? SimpleContainer ?: return
        if (inventory.containerSize != 27) return
        mc.player?.enderChestInventory = PlayerEnderChestContainer().apply {
            repeat(inventory.containerSize) { i ->
                setItem(i, inventory.getItem(i))
            }
        }
    }

    private fun AbstractFurnaceBlockEntity.dataToFurnace(screen: AbstractFurnaceScreen<*>) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun BarrelBlockEntity.dataToBarrelBlock(screen: ContainerScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun BrewingStandBlockEntity.dataToBrewingStand(screen: BrewingStandScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun ChestBlockEntity.dataToChest(screen: ContainerScreen) {
        val facing = blockState.getValue(ChestBlock.FACING) ?: return
        val chestType = blockState.getValue(ChestBlock.TYPE) ?: return
        val containerSlots = screen.getContainerSlots()
        val inventories = containerSlots.partition { it.containerSlot < 27 }

        when (chestType) {
            ChestType.SINGLE -> {
                containerSlots.forEach {
                    setItem(it.containerSlot, it.item)
                }
            }

            ChestType.LEFT -> {
                val pos = blockPos.relative(facing.clockWise)
                val otherChest = level?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    otherChest.setItem(it.containerSlot, it.item)
                }
                inventories.second.forEach {
                    setItem(it.containerSlot - 27, it.item)
                }

                scannedBlockEntities[otherChest.blockPos] = otherChest
            }

            ChestType.RIGHT -> {
                val pos = blockPos.relative(facing.counterClockWise)
                val otherChest = level?.getBlockEntity(pos)
                if (otherChest !is ChestBlockEntity) return

                inventories.first.forEach {
                    setItem(it.containerSlot, it.item)
                }
                inventories.second.forEach {
                    otherChest.setItem(it.containerSlot - 27, it.item)
                }

                scannedBlockEntities[otherChest.blockPos] = otherChest
            }
        }
    }

    private fun DispenserBlockEntity.dataToDispenserOrDropper(screen: DispenserScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun HopperBlockEntity.dataToHopper(screen: HopperScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun ShulkerBoxBlockEntity.dataToShulkerBox(screen: ShulkerBoxScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
        }
    }

    private fun LecternBlockEntity.dataToLectern(screen: LecternScreen) {
        book = screen.menu.book
    }

    private fun CrafterBlockEntity.dataToCrafter(screen: CrafterScreen) {
        screen.getContainerSlots().forEach {
            setItem(it.containerSlot, it.item)
            setSlotState(it.containerSlot, !isSlotDisabled(it.containerSlot))
        }
    }

    private fun AbstractContainerScreen<*>.getContainerSlots() = menu.slots.filter { it.container !is Inventory }
}
