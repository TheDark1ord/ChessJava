package chess;

import org.junit.Test;

import chess.Logic.ChessBoard;
import chess.Moves.Move;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;

public class ChessTests {
    // Data was taken from https://www.chessprogramming.org/Perft_Results
    private static List<Long> pos1MoveCount = Arrays.asList(0L, 20L, 400L, 8902L, 197281L, 4865609L, 119060324L);
    private static List<Long> pos3MoveCount = Arrays.asList(0L, 14L, 191L, 2812L, 43238L, 674624L, 11030083L);
    private static List<Long> pos5MoveCount = Arrays.asList(0L, 44L, 1486L, 62379L, 2103487L, 89941194L);
    private static String pos1FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static String pos3FEN = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 0";
    private static String pos5FEN = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";

    @Test
    public void compareResults() {
        ChessBoard board = new ChessBoard(pos1FEN);
        System.out.println(countMoves(board, 1));
        System.out.println(countMoves(board, 2));
        System.out.println(countMoves(board, 3));
        System.out.println(countMoves(board, 4));
        System.out.println(countMoves(board, 5));
        //System.out.println(countMoves(board, 6));
    }

    private long countMoves(ChessBoard board, int depth) {
        if (depth == 0) {
            return 1;
        }

        var wrapper = new Object() {
            long moveCount = 0;
        };
        Stream<Move> allMoves = board.getAllMoves();
        allMoves.forEach((Move move) -> {
            board.makeAMove(move);
            long curMoves = countMoves(board, depth - 1);

            wrapper.moveCount += curMoves;
            //if (depth == 2) {
            //    System.out.println(String.format("%s: %d", move.toString(), curMoves));
            //}

            board.undoMove();
        });
        return wrapper.moveCount;
    }
}
