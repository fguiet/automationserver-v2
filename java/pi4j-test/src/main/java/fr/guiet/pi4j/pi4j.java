package fr.guiet.pi4j;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;

public class pi4j {
   public static void main (String[] args) {
	   System.out.println("Hello World");
	   
	   final GpioController gpio = GpioFactory.getInstance();
	   GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);
	   pin.high();
	   
	   try {
		Thread.sleep(2000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   pin.low();
	   
	   gpio.shutdown();	  	  
   }
}