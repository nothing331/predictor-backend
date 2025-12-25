package loogic;

import java.util.*;

public class bettingLogic {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter Number of Players");
        int numberOfPlayers = sc.nextInt();
        System.out.print("Enter Number of Rounds");
        int numberOfRounds = sc.nextInt();
        gameTurn(numberOfPlayers, numberOfRounds, sc);
    }

    public static void gameTurn(int numberOfPlayers, int numberOfRounds, Scanner sc) {
        int rounds = 0;
        int[] bets = new int[numberOfPlayers];
        while (rounds < numberOfRounds) {
            int i = 0;
            while (i < numberOfPlayers) {
                System.out.println("Player " + (i + 1) + " Enter your bet");
                int bet = sc.nextInt();
                bets[i] = bets[i] + bet;
                i++;
            }
            betCalculation(bets);
            rounds++;
        }

    }

    public static void betCalculation(int[] bets) {
        int sum = 0;
        for (int i = 0; i < bets.length; i++) {
            sum = sum + bets[i];
        }
        if (sum == 0) {
            System.out.println("Total bets are 0, cannot calculate shares.");
            return;
        }
        for (int i = 0; i < bets.length; i++) {
            System.out.println("Share of Player " + (i + 1) + " is " + ((double) bets[i] / sum));
        }
    }
}
// heyhjjjhj