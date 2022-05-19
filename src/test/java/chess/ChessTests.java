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
    private static List<Long> pos1MoveCount = Arrays.asList(0L, 20L, 400L, 8902L, 197281L, 4865609L);
    private static List<Long> pos5MoveCount = Arrays.asList(0L, 44L, 1486L, 62379L, 2103552L);
    private static String pos1FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static String pos5FEN = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";

    @Test
    public void position1Test() {
        ChessBoard board = new ChessBoard(pos1FEN);
        MoveGeneration generator = new MoveGeneration(board);

        Assert.assertTrue(countMoves(generator, 1) == pos1MoveCount.get(1));
        System.out.println("Pos - 1, depth - 1 -- DONE");

        Assert.assertTrue(countMoves(generator, 2) == pos1MoveCount.get(2));
        System.out.println("Pos - 1, depth - 2 -- DONE");

        Assert.assertTrue(countMoves(generator, 3) == pos1MoveCount.get(3));
        System.out.println("Pos - 1, depth - 3 -- DONE");

        Assert.assertTrue(countMoves(generator, 4) == pos1MoveCount.get(4));
        System.out.println("Pos - 1, depth - 4 -- DONE");

        Assert.assertTrue(countMoves(generator, 5) == pos1MoveCount.get(5));
        System.out.println("Pos - 1, depth - 5 -- DONE");
    }

    @Test
    public void position5Test() {
        ChessBoard board = new ChessBoard(pos5FEN);
        MoveGeneration generator = new MoveGeneration(board);

        Assert.assertTrue(countMoves(generator, 1) == pos5MoveCount.get(1));
        System.out.println("Pos - 5, depth - 1 -- DONE");

        Assert.assertTrue(countMoves(generator, 2) == pos5MoveCount.get(2));
        System.out.println("Pos - 5, depth - 2 -- DONE");

        Assert.assertTrue(countMoves(generator, 3) == pos5MoveCount.get(3));
        System.out.println("Pos - 5, depth - 3 -- DONE");

        Assert.assertTrue(countMoves(generator, 4) == pos5MoveCount.get(4));
        System.out.println("Pos - 5, depth - 4 -- DONE");
    }



    private static List<String> randomFENs = Arrays.asList(
        "rn1qkb1r/p2ppppp/bp3n2/2p5/2P5/5NP1/PP1PPP1P/RNBQKB1R w KQkq - 1 5",
        "r1bqkb1r/ppp2ppp/2n2n2/3pp3/2B1P3/3P4/PPPN1PPP/R1BQK1NR w KQkq - 0 5",
        "rnbqk1nr/pp2bppp/4p3/2pp4/P7/3P1NP1/1PP1PP1P/RNBQKB1R w KQkq - 1 5",
        "rnbqk1nr/pp2ppbp/2p3p1/3p4/P2P4/5NP1/1PP1PP1P/RNBQKB1R w KQkq - 1 5",
        "rn1qkbnr/ppp2ppp/4b3/3p4/8/P4N2/1PPP1PPP/RNBQKB1R w KQkq - 1 5"
    );
    @Test
    public void FENtest() {
        ChessBoard board = new ChessBoard();
        for (int i = 0; i < randomFENs.size(); i++) {
            board.setPosition(randomFENs.get(i));
            Assert.assertEquals(randomFENs.get(i), board.toFEN());
        }
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

            wrapper.moveCount += curMoves;
            generator.undoMove();
        });
        return wrapper.moveCount;
    }
}

