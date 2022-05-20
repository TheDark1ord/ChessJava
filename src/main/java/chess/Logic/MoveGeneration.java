package chess.Logic;

import chess.Vector;
import chess.Logic.ChessBoard.GameResult;
import chess.Logic.ChessBoard.KingStatus;
import chess.Logic.ChessBoard.KingStatus.CheckState;
import chess.Logic.ChessPiece.Name;
import chess.Moves.Castling;
import chess.Moves.Move;
import chess.Moves.Promotion;
import chess.Moves.Castling.Side;
import chess.Moves.Promotion.PromoteTo;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveGeneration {
    public ChessBoard chessBoard;

    public MoveGeneration(ChessBoard chessBoard) {
        this.chessBoard = chessBoard;
    }

    // Checks if this move is possible, then updates some variables like
    // chess board, piece position, clocks, etc.
    public boolean makeAMove(Move move) {
        if (!getPieceMoves(move.piece).contains(move))
            return false;
        chessBoard.addBoardState();

        if (move.piece.getName() == Name.PAWN) {
            chessBoard.resetHalfmoveClock();

            if (move instanceof Promotion) {
                move.captured = chessBoard.getPiece(move.to);
                promote((Promotion) move);

                chessBoard.enPassant = null;

                chessBoard.incrementClocks();
                calculatePossition();
                endGame();

                return true;
            } else if (move.to.y == 0 || move.to.y == 7) {
                Promotion prom = new Promotion(move.piece, move.from, move.to, chessBoard.getPiece(move.to),
                        PromoteTo.INPUT);
                promote(prom);

                chessBoard.enPassant = null;

                chessBoard.incrementClocks();
                calculatePossition();
                endGame();

                return true;
            }
        }

        Castling castling = null;
        if (move.piece.getName() == Name.KING) {
            // If a king moved castling is no longer possible
            castling = castle(move);

            // Update king pos
            chessBoard.getStatus(chessBoard.whiteToMove()).kingPos = move.to;

            chessBoard.updateCastlingRights(move.piece.color, Side.SHORT);
            chessBoard.updateCastlingRights(move.piece.color, Side.LONG);

            // If a rook moved, update castling rights
        } else if (move.piece.getName() == Name.ROOK) {
            if (move.from.x == 0) {
                chessBoard.updateCastlingRights(move.piece.color, Side.LONG);
            } else if (move.from.x == 7) {
                // Short castle
                chessBoard.updateCastlingRights(move.piece.color, Side.SHORT);
            }
        }

        if (castling != null) {
            chessBoard.trackMove(castling);
            chessBoard.enPassant = null;
        } else {
            // If a capture was made, then the move was already added
            if (!capture(move)) {
                chessBoard.trackMove(move);
            }
            chessBoard.enPassant = null;
            doubleMove(move);
        }

        chessBoard.movePiece(move);
        chessBoard.incrementClocks();

        calculatePossition();
        endGame();

        return true;
    }

    public void undoMove() {
        Move toUndo = chessBoard.popLastMove();
        if (toUndo == null) {
            return;
        }
        chessBoard.loadBoardState();
        chessBoard.decrementPosHash();
        chessBoard.decrementClocks();

        if (toUndo instanceof Promotion) {
            Promotion prom = (Promotion) toUndo;

            ChessPiece newPiece = chessBoard.chessBoard[prom.to.y][prom.to.x];
            chessBoard.chessBoardList.remove(newPiece);

            chessBoard.chessBoard[prom.to.y][prom.to.x] = prom.captured;
            chessBoard.chessBoard[prom.from.y][prom.from.x] = prom.piece;
            chessBoard.chessBoardList.add(prom.piece);
            if (prom.captured != null) {
                chessBoard.chessBoardList.add(prom.captured);
            }

            calculatePossition();
            return;
        } else if (toUndo instanceof Castling) {
            Castling cstl = (Castling) toUndo;

            if (cstl.side == Side.SHORT) {
                Vector rookPos = new Vector(toUndo.from.x + 1, toUndo.from.y);
                Move undoCastling = new Move(
                        chessBoard.getPiece(rookPos),
                        rookPos, new Vector(7, toUndo.from.y));

                chessBoard.movePiece(undoCastling);
            } else {
                Vector rookPos = new Vector(toUndo.from.x - 1, toUndo.from.y);
                Move undoCastling = new Move(
                        chessBoard.getPiece(rookPos),
                        rookPos, new Vector(0, toUndo.from.y));

                chessBoard.movePiece(undoCastling);
            }
            // Essentially a fallthrough
        }
        if (toUndo.piece.getName() == Name.KING) {
            chessBoard.getStatus(chessBoard.whiteToMove()).kingPos = toUndo.from;
        }
        chessBoard.movePiece(new Move(toUndo.piece, toUndo.to, toUndo.from));

        if (toUndo.captured != null) {
            chessBoard.chessBoard[toUndo.captured.pos.y][toUndo.captured.pos.x] = toUndo.captured;
            chessBoard.chessBoardList.add(toUndo.captured);
        }

        calculatePossition();
    }

    public List<Move> getPieceMoves(ChessPiece piece) {
        if (chessBoard.gameResult != ChessBoard.GameResult.NONE) {
            return new LinkedList<Move>();
        }

        // Null and color
        ChessPiece.Color currentColor = chessBoard.getCurrentColor();
        if (piece == null || piece.color != currentColor)
            return new LinkedList<Move>();

        // Check and pin
        KingStatus kSt = chessBoard.getStatus(chessBoard.whiteToMove());
        if (kSt.checkState == KingStatus.CheckState.DOUBLE)
            return new LinkedList<Move>();

        return getLegalMoves(piece, kSt);
    }

    public Stream<Move> getAllMoves() {
        Stream<Move> retStream = Stream.empty();
        for (ChessPiece piece : chessBoard) {
            retStream = Stream.concat(retStream, getPieceMoves(piece).stream());
        }
        return retStream;
    }

    // Calculate moves and update KingStatus
    void calculatePossition() {
        chessBoard.WKingSt.resetStatus();
        chessBoard.BKingSt.resetStatus();

        for (ChessPiece piece : chessBoard) {
            piece.pinnedDirection = new LinkedList<>();
            piece.generatePossibleMoves();
            chessBoard.getStatus(ChessPiece.invert(piece.color)).attackedSquares.or(piece.attackedSquares);
        }

        chessBoard.updateKingStatus(chessBoard.WKingSt);
        chessBoard.updateKingStatus(chessBoard.BKingSt);
    }

    private void promote(Promotion promotion) {
        ChessPiece newPiece;

        chessBoard.trackMove(promotion);

        switch (promotion.promoteTo) {
            case INPUT:
            case QUEEN:
            default:
                newPiece = new Queen(promotion.to, promotion.piece.color, chessBoard);
                break;
            case ROOK:
                newPiece = new Rook(promotion.to, promotion.piece.color, chessBoard);
                break;
            case BISHOP:
                newPiece = new Bishop(promotion.to, promotion.piece.color, chessBoard);
                break;
            case KNIGHT:
                newPiece = new Knight(promotion.to, promotion.piece.color, chessBoard);
                break;
        }

        chessBoard.chessBoard[promotion.to.y][promotion.to.x] = newPiece;
        chessBoard.chessBoard[promotion.from.y][promotion.from.x] = null;

        chessBoard.chessBoardList.add(newPiece);

        chessBoard.chessBoardList.remove(promotion.piece);
        if (promotion.captured != null) {
            chessBoard.chessBoardList.remove(promotion.captured);
        }
    }

    // Check if the move is castling to move a rook
    private Castling castle(Move move) {
        if (move.to.equals(new Vector(move.from.x - 2, move.from.y))) {
            // Castle long
            Vector rookFrom = new Vector(0, move.from.y);
            Vector rookTo = new Vector(move.from.x - 1, move.from.y);
            chessBoard.movePiece(new Move(chessBoard.getPiece(rookFrom), rookFrom, rookTo));

            return new Castling(move.piece, move.from, move.to, Castling.Side.LONG);
        } else if (move.to.equals(new Vector(move.from.x + 2, move.from.y))) {
            // Castle short
            Vector rookFrom = new Vector(7, move.from.y);
            Vector rookTo = new Vector(move.from.x + 1, move.from.y);
            chessBoard.movePiece(new Move(chessBoard.getPiece(rookFrom), rookFrom, rookTo));

            return new Castling(move.piece, move.from, move.to, Castling.Side.SHORT);
        }

        return null;
    }

    // Check for capture
    private boolean capture(Move move) {
        ChessPiece capturedPiece = chessBoard.getPiece(move.to);

        if (capturedPiece != null) {
            chessBoard.trackMove(new Move(move.piece, move.from, move.to, capturedPiece));

            // Update castling rights if a rook was captured
            if (capturedPiece.getName() == Name.ROOK) {
                if (capturedPiece.pos.x == 0) {
                    // Long castle
                    chessBoard.updateCastlingRights(capturedPiece.color, Side.LONG);
                } else if (capturedPiece.pos.x == 7) {
                    // Short castle
                    chessBoard.updateCastlingRights(capturedPiece.color, Side.SHORT);
                }
            }
            chessBoard.chessBoardList.remove(capturedPiece);
            // Reset clock (it will be later incremented to 0)
            chessBoard.resetHalfmoveClock();
            return true;

            // Check for enPassant capture
        } else if (move.piece.getName() == Name.PAWN && chessBoard.enPassant != null
                && chessBoard.enPassant.equals(move.to)) {
            capturedPiece = chessBoard.getPiece(new Vector(chessBoard.enPassant.x, move.from.y));

            chessBoard.trackMove(new Move(move.piece, move.from, move.to, capturedPiece));

            chessBoard.chessBoardList.remove(capturedPiece);
            chessBoard.chessBoard[capturedPiece.pos.y][capturedPiece.pos.x] = null;
            // Reset clock
            chessBoard.resetHalfmoveClock();

            return true;
        }
        return false;
    }

    // If a pawn made double move, update enPassant
    private void doubleMove(Move move) {
        // If the pawn is white forward move will increase pos.y
        // If the pawn is black forward move will decrease pos.y
        int forMov = move.piece.color == ChessPiece.Color.WHITE ? 1 : -1;

        if (move.piece.getName() != Name.PAWN) {
            return;
        }

        // Pawn made a double move
        if (new Vector(move.from.x, move.from.y + forMov * 2).equals(move.to)) {
            // Mark the square behind the pawn
            chessBoard.enPassant = new Vector(move.piece.pos.x, move.from.y + forMov);
        }
    }

    // Chech if the game should conclude
    private void endGame() {
        KingStatus kSt = chessBoard.getStatus(chessBoard.whiteToMove());

        // If no moves are possible
        if (getAllMoves().count() == 0) {
            if (kSt.checkState != CheckState.NONE) {
                chessBoard.gameResult = chessBoard.whiteToMove() ? GameResult.BLACK_WON : GameResult.WHITE_WON;
            } else {
                chessBoard.gameResult = GameResult.DRAW;
            }
            return;
        }

        // Fifty move rule
        if (chessBoard.halfMoveClock == 100) {
            chessBoard.gameResult = GameResult.DRAW;
            return;
        }

        chessBoard.checkRepetition();
    }

    // Get pseudo-legal moves of a piece and return only legal moves
    private List<Move> getLegalMoves(ChessPiece piece, KingStatus kSt) {
        // If a king is under check, add all moves, that block that check
        if (piece.getName() == ChessPiece.Name.KING) {
            return piece.possibleMoves.stream()
                    .filter((Move move) -> {
                        return !chessBoard.isUnderAttack(move.to, piece.color);
                    })
                    .collect(Collectors.toList());
        } else if (kSt.pinnedPieces.get(piece.pos.y * 8 + piece.pos.x)) {

            return piece.possibleMoves.stream().filter((Move move) -> {
                return piece.pinnedDirection.stream().allMatch((Vector direction) -> {
                    // Return true if the move is along the pinned direction
                    return Vector.sub(move.to, move.from).isMultiple(direction);
                });
            })
                    .collect(Collectors.toList());
        } else if (kSt.checkState == KingStatus.CheckState.SINGLE) {
            return piece.possibleMoves.stream()
                    .filter((Move move) -> {
                        return kSt.toBlockSq.get(move.to.y * 8 + move.to.x);
                    })
                    .collect(Collectors.toList());
        } else {
            return piece.possibleMoves;
        }
    }
}
