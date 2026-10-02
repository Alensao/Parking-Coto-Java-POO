
package com.mycompany.parking.coto.java.poo;
import java.util.Scanner;
import service.ParkingLot;
import ui.ConsoleInput;
import ui.ConsoleMenu;
public class ParkingCotoJavaPOO {
public static void main(String[] args) {
    ParkingLot parkingLot = new ParkingLot("Parking Coto");
    ConsoleInput input = new ConsoleInput(new Scanner(System.in));
    new ConsoleMenu(parkingLot, input).start();
}
}
