package com.example.fruitylicious.domain.usecase.staff

import com.example.fruitylicious.data.repository.StaffLogRepository
import javax.inject.Inject

class ClockInUseCase @Inject constructor(
    private val staffLogRepository: StaffLogRepository
) {

    suspend operator fun invoke(
        userId: Int,
        branchId: Int,
        image: String?
    ): Result<Unit> {
        return staffLogRepository.clockIn(
            userId = userId,
            branchId = branchId,
            image = image
        )
    }
}