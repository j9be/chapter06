package packt.java9.by.example.mastermind.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import packt.java9.by.example.mastermind.*;
import packt.java9.by.example.mastermind.lettered.LetteredColorFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Mastermind extends HttpServlet {
    private static final int NR_COLORS = 6;
    private static final int NR_COLUMNS = 4;
    private static final Logger log = LoggerFactory.getLogger(Mastermind.class);

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {
        final ColorManager manager =
                new ColorManager(NR_COLORS, new LetteredColorFactory());
        Table table = new Table(NR_COLUMNS, manager);
        Game game = buildGameFromRequest(request, manager, table);
        Guesser guesser = new UniqueGuesser(table);
        Guess newGuess = guesser.guess();
        if (game.isFinished() || newGuess == Guess.none) {
            displayGameOver(response,table);
        } else {
            log.debug("Adding new guess {} to the game", newGuess);
            game.addGuess(newGuess, 0, 0);
            displayGame(response, table);
        }
    }

    private void displayGameOver(HttpServletResponse response, Table table) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println(tableToHtml(table));
        out.println("</form>");
        out.println("Game finished, no more guesses");
        out.println("</body></head></html>");
    }

    private void displayGame(HttpServletResponse response, Table table) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println(tableToHtml(table));
        out.println(tag("input", "type", "submit", "value", "submit"));
        out.println("</form></body></head></html>");

    }

    private Game buildGameFromRequest(HttpServletRequest request,
                                      ColorManager manager,
                                      Table table) {
        Guess secret = new Guess(new Color[NR_COLUMNS]);
        Game game = new Game(table, secret);
        for (int row = 0;
             request.getParameter(paramNameGuess(row, 0)) != null;
             row++) {
            Color[] colors = getRowColors(request, manager, row);
            Guess guess = new Guess(colors);
            int full = Integer.parseInt(request.getParameter("full" + row));
            int partial = Integer.parseInt(request.getParameter("partial" + row));
            log.debug("Adding guess to game");
            game.addGuess(guess, full, partial);
        }
        return game;
    }

    private Color[] getRowColors(HttpServletRequest request, ColorManager manager, int row) {
        Color[] colors = new Color[NR_COLUMNS];
        for (int column = 0; column < NR_COLUMNS; column++) {
            String letter = request.getParameter(paramNameGuess(row, column));
            colors[column] = colorFrom(letter, manager);
            log.debug("Processing guess{}{} = {}", row, column, colors[column]);
        }
        return colors;
    }

    private String paramNameFull(int row) {
        return "full" + row;
    }

    private String paramNamePartial(int row) {
        return "partial" + row;
    }

    private String paramNameGuess(int row, int column) {
        return "guess" + row + column;
    }

    private String tableToHtml(Table table) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>");
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"colors.css\">");
        sb.append("<title>Mastermind guessing</title>");
        sb.append("<body>");
        sb.append(tag("form", "method", "POST", "action", "master"));

        for (int row = 0; row < table.nrOfRows(); row++) {
            for (int column = 0; column < NR_COLUMNS; column++) {
                sb.append(colorToHtml(table.getColor(row, column), row, column));
            }
            sb.append(inputBox(paramNameFull(row), "" + table.getFull(row)));
            sb.append(inputBox(paramNamePartial(row), "" + table.getPartial(row)));
            sb.append("<p>");
        }
        return sb.toString();
    }

    private String inputBox(String name, String value) {
        return tag("input", "type", "text", "name", name, "value", value, "size", "1");
    }

    private String colorToHtml(Color color, int row, int column) {
        return tag("input", "type", "hidden", "name", paramNameGuess(row, column),
                "value", color.toString()) +
                tag("div", "class", "color" + color) +
                tag("/div") +
                tag("div", "class", "spacer") +
                tag("/div");
    }

    private String tag(String tagName, String... attributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append((tagName));
        for (int i = 0; i < attributes.length; i += 2) {
            sb.append(" ").
                    append(attributes[i]).
                    append("=\"").
                    append(attributes[i + 1]).
                    append("\"");
        }
        sb.append(">");
        return sb.toString();
    }

    private Color colorFrom(String letter, ColorManager manager) {
        Color color = manager.firstColor();
        while (!color.toString().equals(letter)) {
            if (manager.thereIsNextColor(color)) {
                color = manager.nextColor(color);
            } else {
                return null;
            }
        }
        return color;

    }

}