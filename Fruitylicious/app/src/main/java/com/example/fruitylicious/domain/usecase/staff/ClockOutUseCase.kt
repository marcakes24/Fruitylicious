package com.example.fruitylicious.domain.usecase.staff

import com.example.fruitylicious.data.repository.StaffLogRepository
import javax.inject.Inject

class ClockOutUseCase @Inject constructor(
    private val staffLogRepository: StaffLogRepository
) {

    suspend operator fun invoke(
        userId: Int,
        branchId: Int
    ): Result<Unit> {
        return staffLogRepository.clockOut(
            userId = userId,
            branchId = branchId
        )
    }
}