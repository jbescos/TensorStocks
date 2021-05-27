package com.jbescos.test;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

// Java extension packages
import javax.swing.JFrame;

import com.jbescos.common.Utils;

public class Painter extends JFrame {
	
	private static final StringBuilder CSV = new StringBuilder("DATE,SYMBOL,PRICE\r\n");
	private static final long START = new Date(0).getTime();
	private static final long HOUR = 3600000;
	
	private int x = 0, y = 0;

	public Painter() {

		super("CSV generator");
		getContentPane().add(new Label("Drag the mouse to draw"), BorderLayout.SOUTH);
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent event) {
				if (event.getX() > x) {
					x = event.getX();
					y = event.getY();
					long newDate = START + (x * HOUR);
					CSV.append(Utils.fromDate(Utils.FORMAT_SECOND, new Date(newDate))).append(",").append("SYMBOL").append(",").append((double)(getHeight() - y)).append("\r\n");
					repaint();
				}
			}
		});
		setSize(Toolkit.getDefaultToolkit().getScreenSize());
		setVisible(true);
	}

	public void paint(Graphics g) {
		g.fillOval(x, y, 4, 4);
	}

	public static void main(String args[]) {
		Painter application = new Painter();
		application.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				System.out.println(CSV.toString());
				System.exit(0);
			}
		});
	}
}
