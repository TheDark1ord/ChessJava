package chess;

import org.junit.Test;

import chess.Logic.ChessBoard;
import chess.Logic.MoveGeneration;
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
    public void posistion1Test() {
        ChessBoard board = new ChessBoard("rnbqkbnr/pppppppp/8/8/8/P7/1PPPPPPP/RNBQKBNR b KQkq - 0 1");
        MoveGeneration generator = new MoveGeneration(board);

        //System.out.println(countMoves(generator, 1));
        //System.out.println(countMoves(generator, 2));
        System.out.println(countMoves(generator, 3));
        //System.out.println(countMoves(generator, 4));
        //System.out.println(countMoves(generator, 5));

        //Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(1));
        //Assert.assertTrue(countMoves(generator, 2) == pos1MoveCount.get(2));
        //Assert.assertTrue(countMoves(generator, 3) == pos1MoveCount.get(3));
        //Assert.assertTrue(countMoves(generator, 4) == pos1MoveCount.get(4));
        // Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(5));
        // Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(6));
    }

    @Test
    public void posistion3Test() {
        ChessBoard board = new ChessBoard(pos3FEN);
        MoveGeneration generator = new MoveGeneration(board);

        //System.out.println(countMoves(generator, 1));
        //System.out.println(countMoves(generator, 2));
        //System.out.println(countMoves(generator, 3));
        //System.out.println(countMoves(generator, 4));
        System.out.println(countMoves(generator, 5));

        //Assert.assertTrue(countMoves(generator, 1) == pos3MoveCount.get(1));
        //Assert.assertTrue(countMoves(generator, 2) == pos3MoveCount.get(2));
        //Assert.assertTrue(countMoves(generator, 3) == pos3MoveCount.get(3));
        //Assert.assertTrue(countMoves(generator, 4) == pos3MoveCount.get(4));
        // Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(5));
        // Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(6));
    }

    private long countMoves(MoveGeneration generator, int depth) {
        if (depth == 0) {
            return 1;
        }

        var wrapper = new Object() {
            long moveCount = 0;
        };
        Stream<Move> allMoves = generator.getAllMoves();
        allMoves.forEach((Move move) -> {
            generator.makeAMove(move);

            long curMoves = countMoves(generator, depth - 1);

            if (depth == 3) {
                System.out.println(String.format("%s : %d", move.toString(), curMoves));
            }

            wrapper.moveCount += curMoves;
            generator.undoMove();
        });
        return wrapper.moveCount;
    }
}
