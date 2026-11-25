package com.rojas.syncpass.domain

import com.rojas.syncpass.domain.model.DeviceLocation

interface LocationSource {
    suspend fun currentLocation(): Result<DeviceLocation>
}
