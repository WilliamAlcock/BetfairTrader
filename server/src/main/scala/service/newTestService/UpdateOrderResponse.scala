package service.newTestService

import domain.UpdateInstructionReport

case class UpdateOrderResponse[T](result: T, report: UpdateInstructionReport)

