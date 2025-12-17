package org.example;

import org.example.application.ServiceLocator;
import org.example.presentation.MenuController;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ServiceLocator serviceLocator = new ServiceLocator();
        MenuController menuController = new MenuController(serviceLocator, scanner);

        menuController.startApplication();
        scanner.close();
    }
}
