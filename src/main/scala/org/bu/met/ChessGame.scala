package org.bu.met

import java.io.FileWriter

import org.bu.met.types._

class ChessGame(var activePieces: Seq[(ChessPiece, Position)], var turn: Color){

  private var moveVectorOpt: Option[MoveVector] = None
  private var stateVectorOpt: Option[Seq[Int]] = None

  activePieces.foreach{case(_, (row, col)) =>
    require(range.contains(row) && range.contains(col))
  }

  val board: Board = {
    val board: Board = Array.fill(8)(Array.fill(8)(None))
    activePieces.foreach{case(piece, (x,y))=>
      val (row,col) = toRowCol(x,y)
      board(row)(col) = Some(piece)
    }
    board
  }

  def updateBoard(): Unit = {
    val piecesToMove: Seq[(ChessPiece, Position)] = activePieces.filter{case(piece, _) => piece.color == turn}
    val kingOpt: Option[(ChessPiece, (Int, Int))] = piecesToMove.find{case (piece, _) => piece.isInstanceOf[King]}
    var kingInCheck = false
    val (selectedPiece, (oldX,oldY)) = kingOpt match {
      case Some((king: King, (x,y))) =>
        kingInCheck = inCheck(x,y, board, turn)
        if(kingInCheck) (king, (x,y))
        else choose(piecesToMove.iterator)
      // in some test cases there are no kings, but this wouldn't be realistic
      case _ => choose(piecesToMove.iterator)
    }
    val possibleMoves: Seq[Position] =
      getMovesForPiece(selectedPiece, oldX, oldY, board).filter{case (a,b) => !inCheck(a,b, board, turn)}
    turn = if (turn.equals(White)) Black else White // switch turns
    if(possibleMoves.nonEmpty) {
      val (newX, newY) = choose(possibleMoves.iterator)
      moveVectorOpt = Some(MoveVector(selectedPiece.stateVectorIndex, newX, newY))
      val (newRow, newCol) = toRowCol(newX, newY)
      val (oldRow, oldCol) = toRowCol(oldX, oldY)
      activePieces = activePieces.filter { case (_, (x, y)) => x != oldX || y != oldY }
      if (board(newRow)(newCol).nonEmpty)
        activePieces = activePieces.filter { case (_, (x, y)) => x != newX || y != newY }
      selectedPiece match {
        case p: Pawn => attemptPromotePawn(p, (oldX, oldY))
        case _ =>
      }
      activePieces = activePieces :+(selectedPiece, (newX, newY))
      board(newRow)(newCol) = Some(selectedPiece)
      board(oldRow)(oldCol) = None
      stateVectorOpt = generateStateVector(moveVectorOpt)
    }
    else if(kingInCheck){
      saveMoveAndState(moveVectorOpt, stateVectorOpt)
      println(s"$turn wins!!!")
    }
    else println(s"stalemate, $turn cannot move.")
  }

  private def saveMoveAndState(moveVectorOpt: Option[MoveVector], stateVectorOpt: Option[Seq[Int]]): Unit ={
    (stateVectorOpt, moveVectorOpt) match {
      case (Some(sv), Some(mv)) =>
        val fw = new FileWriter("training_data.csv")
        fw.write(sv.mkString(",") + s",${mv.toString}")
        fw.close()
      case _ =>
    }
  }

  private def generateStateVector(moveVectorOpt: Option[MoveVector]) = moveVectorOpt match {
    case Some(mv) =>
      val turnInt = if(turn == Black) 0 else 1
      val statesArray = Array.fill(32)(PieceState(1,0,0))
      activePieces.foreach{ case(piece,(x,y)) =>
        statesArray(piece.stateVectorIndex) = PieceState(0,x,y)
      }
      Some(statesArray.toSeq.flatMap{ ps => Seq(ps.taken, ps.xPos, ps.yPos)} ++ Seq(turnInt))
    case None => None
  }

  // TODO: Placeholder for actual chess-bot, used to generate initial training data
  private def choose[A](it: Iterator[A]): A =
    it.zip(Iterator.iterate(1)(_ + 1)).reduceLeft((row, col) =>
      if (util.Random.nextInt(col._2) == 0) col else row
    )._1

  private def attemptPromotePawn(p: Pawn, position: Position): Unit ={
    def promotablePawn(col: Int, color: Color) =
      (color == White && col == 7) || (color == Black && col == 0)
    val (row,col) = position

    if(promotablePawn(col, p.color)){
      val possiblePieces = List(Knight(p.color, p.stateVectorIndex), Queen(p.color, p.stateVectorIndex))
      val promotedPiece = choose(possiblePieces.iterator)
      board(row)(col) = Some(promotedPiece)
      activePieces = activePieces.filter{case( piece, pos) => piece match {
        case p: Pawn => pos == (row,col)
        case _ => false
      }}
      activePieces = activePieces :+ ((promotedPiece, (row,col)))
    }
  }

  def printBoard(): Unit ={
    for(row <- range){
      println()
      for(col <- range) {
        board(row)(col) match {
          case None => print("[            ]")
          case Some(piece) => print(s"[$piece]")
        }
      }
    }
  }
}
