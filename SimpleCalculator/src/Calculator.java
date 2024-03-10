import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Calculator extends JFrame {
    private JTextField output;
    private double number = 0;
    private String operator = "";

    private Font font = new Font("MONOSPACED", Font.BOLD, 20);

    public Calculator() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        output = new JTextField();
        output.setFont(font);
        add(output, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 4));

        String[] buttonLabels = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "0", "C", "=", "+"
        };

        // ActionListener for buttons
        ActionListener buttonActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                if (command.matches("\\d")) {
                    output.setText(output.getText() + command);
                } else if (command.equals("C")) {
                    output.setText("");
                } else if (command.matches("[+\\-*/]")) {
                    number = Double.parseDouble(output.getText());
                    operator = command;
                    output.setText("");
                } else if (command.equals("=")) {
                    double result = calculateResult(number, operator, Double.parseDouble(output.getText()));
                    output.setText(String.valueOf(result));
                }
            }
        };

        // Add buttons to the panel with the ActionListener
        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setFont(font);
            buttonPanel.add(button);
            button.addActionListener(buttonActionListener);
        }

        // KeyListener for the text field to restrict input
        output.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char inputChar = e.getKeyChar();
                if (!Character.isDigit(inputChar) && "+-*/".indexOf(inputChar) == -1) {
                    e.consume();  // Ignore non-numeric and non-operator characters
                }
            }
        });

        // Set focus on the calculator window
        requestFocusInWindow();
        output.requestFocusInWindow();

        add(buttonPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private double calculateResult(double num1, String operator, double num2) {
        switch (operator) {
            case "+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "*":
                return num1 * num2;
            case "/":
                if (num2 != 0) {
                    return num1 / num2;
                } else {
                    JOptionPane.showMessageDialog(this, "Error: Division by zero", "Error", JOptionPane.ERROR_MESSAGE);
                    return 0;
                }
            default:
                return 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Calculator calc = new Calculator();
            calc.setVisible(true);
        });
    }
}
