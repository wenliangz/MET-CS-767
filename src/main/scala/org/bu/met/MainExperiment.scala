package org.bu.met

import org.bu.met.data.DataGenerator
import org.bu.met.types._

object MainExperiment {
  def main(args: Array[String]) {
//    val model = new InferenceModel
//    model.train("training_data.csv")
    val pieces = DataGenerator.testCase4
    val game = new ChessGame(pieces, White)
    game.updateBoard()
    game.updateBoard()
//    inCheck(7,6, game.board, pieces, Black)
//    game.runGame(10, Some(model))
//    println(game.numMoves)
//    val move = model.computeMoveVector(StateVector(pieces, White))
//    println(move.toReadableMove)
  }
}
