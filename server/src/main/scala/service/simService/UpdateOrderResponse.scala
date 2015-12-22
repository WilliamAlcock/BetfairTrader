package service.simService

import domain.UpdateInstructionReport

case class UpdateOrderResponse[T](result: T, report: UpdateInstructionReport)

