package test.java.com.jetbrains.parser;

import main.java.com.jetbrains.parser.BuildVisitor;
import main.java.com.jetbrains.parser.VcgrammarLexer;
import main.java.com.jetbrains.parser.VcgrammarParser;
import main.java.com.jetbrains.term.expr.Expression;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

import static main.java.com.jetbrains.term.expr.Expression.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ParserTest {
    @Test
    public void parserLam() {
        Expression expr = parseExpr("\\x y z -> y");
        assertEquals(Lam("x", Lam("y", Lam("z", Index(1)))), expr);
    }

    @Test
    public void parserLam2() {
        Expression expr = parseExpr("\\x y -> (\\z w -> y z) y");
        assertEquals(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Index(2), Index(1)))), Index(0)))), expr);
    }

    @Test
    public void parserPi() {
        Expression expr = parseExpr("(x y z : N) (w t : N -> N) -> (a b : ((c : N) -> N c)) -> N b y w");
        Expression natNat = Pi(Nat(), Nat());
        Expression piNat = Pi("c", Nat(), Apps(Nat(), Index(0)));
        assertEquals(Pi("x", Nat(), Pi("y", Nat(), Pi("z", Nat(), Pi("w", natNat, Pi("t", natNat, Pi("a", piNat, Pi("b", piNat, Apps(Nat(), Index(0), Index(5), Index(3))))))))), expr);
    }

    @Test
    public void parserPi2() {
        Expression expr = parseExpr("(x y : N) (z : N x -> N y) -> N z y x");
        assertEquals(Pi("x'", Nat(), Pi("y'", Nat(), Pi("z'", Pi(Apps(Nat(), Index(1)), Apps(Nat(), Index(0))), Apps(Nat(), Index(0), Index(1), Index(2))))), expr);
    }

    @Test
    public void parserLamOpen() {
        BuildVisitor builder = new BuildVisitor();
        Expression expr = (Expression) builder.visit(parse("\\x -> ((y : N) -> (\\y -> y)) y").expr());
        assertEquals(Lam("x", Apps(Pi("y", Nat(), Lam("y", Index(0))), Var("y"))), expr);
        assertArrayEquals(new String[]{"y"}, builder.getUnknownVariables().toArray(new String[builder.getUnknownVariables().size()]));
    }

    @Test
    public void parserPiOpen() {
        BuildVisitor builder = new BuildVisitor();
        Expression expr = (Expression) builder.visit(parse("(a b : N a) -> N a b").expr());
        Expression natVar = Apps(Nat(), Var("a"));
        assertEquals(Pi("a", natVar, Pi("b", natVar, Apps(Nat(), Index(1), Index(0)))), expr);
        assertArrayEquals(new String[]{"a"}, builder.getUnknownVariables().toArray(new String[builder.getUnknownVariables().size()]));
    }

    @Test
    public void parserDef() {
        BuildVisitor builder = new BuildVisitor();
        builder.visit(parse("x : N = 0; y : N = x;").defs());
        assertArrayEquals(new String[0], builder.getUnknownVariables().toArray(new String[builder.getUnknownVariables().size()]));
    }

    @Test
    public void parserDefType() {
        BuildVisitor builder = new BuildVisitor();
        builder.visit(parse("x : Type0 = N; y : x = 0;").defs());
        assertArrayEquals(new String[0], builder.getUnknownVariables().toArray(new String[builder.getUnknownVariables().size()]));
    }

    private static VcgrammarParser parse(String text) {
        ANTLRInputStream input = new ANTLRInputStream(text);
        VcgrammarLexer lexer = new VcgrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new VcgrammarParser(tokens);
    }

    private static Expression parseExpr(String text) {
        BuildVisitor builder = new BuildVisitor();
        return (Expression) builder.visit(parse(text).expr());
    }
}
