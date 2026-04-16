package com.gayakini.inventory.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InventoryMovementRepository : JpaRepository<InventoryMovement, UUID>
