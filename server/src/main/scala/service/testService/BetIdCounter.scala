/**
 * Created by Alcock on 27/10/2015.
 */

package service.testService

class BetIdCounter {
  private var nextBetId: Int = 0

  def getNextBetId(): String = {
    nextBetId += 1
    return nextBetId.toString()
  }
}
