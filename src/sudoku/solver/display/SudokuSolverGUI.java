package sudoku.solver.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AbstractDocument;

import sudoku.solver.main.SudokuSolver;
import sudoku.solver.main.SudokuSolver.SudokuDisplayer;

/**
 * THIS IS REALLY MESST AND SHOULD BE REFACTORED
 */
public class SudokuSolverGUI implements SudokuDisplayer {
	
	private final SudokuSolver sudokuSolver;
	private final JTextArea logTextArea;
	private final List<JTextField> sudokuIndexTextFieldList;
	private final JLabel solveTimeLabel;
	
	boolean updatingList;
	
	public SudokuSolverGUI() {
		sudokuSolver = new SudokuSolver();
		JFrame jframe = new JFrame();
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setSize(new Dimension(625, 525));
		//sudoku panel
		JPanel sudokuJPanel = new JPanel(new BorderLayout());
		JPanel sudokuGridJPanel = new JPanel(new GridLayout(3, 3, 2, 2));
		sudokuGridJPanel.setPreferredSize(new Dimension(475, 475));
		sudokuGridJPanel.setBackground(Color.BLACK);
		sudokuIndexTextFieldList = new ArrayList<JTextField>(81);
		for(int i = 0; i < 9; i++){
			sudokuGridJPanel.add(createSubSudokuGridJPanel());
		}
		for(int k = 0; k < 3; k++){
			for(int j = 0; j < 3; j++){
				int indexCounter = -1;
				for(int i = 0; i < 9; i++){
					indexCounter++;
					if(indexCounter == 3){
						indexCounter = 0;
					}
					JPanel sudokuSubJPanel = (JPanel) sudokuGridJPanel.getComponent((i / 3 + k * 3));
					sudokuIndexTextFieldList.add((JTextField) sudokuSubJPanel.getComponent((indexCounter + j * 3)));
				}
			}
		}
		JPanel sudokuRowLetterJPanel = new JPanel(new GridLayout(9, 1));
		for(String rowLetter : SudokuSolver.ROWLETTERS){
			sudokuRowLetterJPanel.add(new JLabel(rowLetter));
		}
		JPanel sudokuColNrJPanel = new JPanel(new GridLayout(1, 10));
		for(Integer colNr : SudokuSolver.COLNUMBERS){
			JLabel colNrJLabel = new JLabel("" + colNr);
			colNrJLabel.setBorder(new EmptyBorder(0, 25, 0, 0));
			sudokuColNrJPanel.add(colNrJLabel);
		}
		sudokuJPanel.add(sudokuGridJPanel, BorderLayout.CENTER);
		sudokuJPanel.add(sudokuRowLetterJPanel, BorderLayout.WEST);
		sudokuJPanel.add(sudokuColNrJPanel, BorderLayout.NORTH);
		// controls panel
		JPanel controlsJPanel = new JPanel(new GridLayout(8, 1));
		controlsJPanel.setPreferredSize(new Dimension(150, 475));
		JButton solveBtn = new JButton("Solve");
		solveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.solve();
			}
		});
		controlsJPanel.add(solveBtn);
		JButton resetBtn = new JButton("Reset");
		resetBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.reset();
			}
		});
		controlsJPanel.add(resetBtn);
		JButton checkValidBtn = new JButton("Check");
		checkValidBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.checkValid();
			}
		});
		controlsJPanel.add(checkValidBtn);
		JButton showSolutionBtn = new JButton("Show solution");
		showSolutionBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.showSolution();
			}
		});
		controlsJPanel.add(showSolutionBtn);
		JButton hideSolutionBtn = new JButton("Hide solution");
		hideSolutionBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.hideSolution();
			}
		});
		controlsJPanel.add(hideSolutionBtn);
		JButton hintBtn = new JButton("Show hint");
		hintBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.showHint();
			}
		});
		controlsJPanel.add(hintBtn);
		JPanel seedPanel = new JPanel();
		JTextArea seedTextArea = new JTextArea("Insert sudoku here");
		JScrollPane seedTextAreaScrollPane = new JScrollPane(seedTextArea);
		JButton seedBtn = new JButton("Seed");
		seedBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sudokuSolver.seed(seedTextArea.getText());
			}
		});
		seedPanel.setLayout(new BorderLayout());
		seedPanel.add(seedTextAreaScrollPane, BorderLayout.CENTER);
		seedPanel.add(seedBtn, BorderLayout.SOUTH);
		controlsJPanel.add(seedPanel);
		solveTimeLabel = new JLabel();
		solveTimeLabel.setText("Solve time: 0ms");
		controlsJPanel.add(solveTimeLabel);
		logTextArea = new JTextArea();
		JScrollPane logTextAreaScrollPane = new JScrollPane(logTextArea);
		logTextAreaScrollPane.setPreferredSize(new Dimension(475, 50));
		jframe.setLayout(new BorderLayout());
		jframe.getContentPane().add(sudokuJPanel, BorderLayout.CENTER);
		jframe.getContentPane().add(controlsJPanel, BorderLayout.EAST);
		jframe.getContentPane().add(logTextAreaScrollPane, BorderLayout.SOUTH);
		jframe.setLocationRelativeTo(null);
		jframe.setResizable(false);
		jframe.setVisible(true);
		sudokuSolver.setSudokuSolverDisplayer(this);
	}
	
	/**
	 * 
	 * @return
	 */
	private JPanel createSubSudokuGridJPanel(){
		JPanel subSudokuGridJPanel = new JPanel(new GridLayout(3, 3, 1, 1));
		Font font = new Font("SansSerif", Font.BOLD, 50);
		for(int i = 0; i < 9; i++){
			JTextField sudokuIndexTextField = new JTextField();
	        ((AbstractDocument)sudokuIndexTextField.getDocument()).setDocumentFilter(
	                new DocumentFilter(){
	                	@Override
	                	public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
	                			throws BadLocationException {
	            	    	int documentLength = fb.getDocument().getLength(); 
	            	    	if(text.length() == 0){
	            	    		//emtpy entry
	            	    		super.remove(fb, 0, documentLength);
	            	    		return;
	            	    	}
	            	    	if(!Character.isDigit(text.charAt(0))
	            	    			|| text.charAt(0) == '0'){
	            	    		Toolkit.getDefaultToolkit().beep();
	            	    		return;
	            	    	}
	                        if (documentLength - length + text.length() <= 1){
	                        	//first entry
	                        	super.replace(fb, offset, length, text, attrs);
	                        } else {
	                        	//replacement
	                        	super.remove(fb, 0, documentLength);
	                        	super.insertString(fb, 0, text, attrs);
	                        }
	                        if(!updatingList){
	    						int nextSudokuIndexTextFieldIndex = sudokuIndexTextFieldList.indexOf(sudokuIndexTextField) + 1;
	    						if(nextSudokuIndexTextFieldIndex < sudokuIndexTextFieldList.size()){
	    							sudokuIndexTextFieldList.get(nextSudokuIndexTextFieldIndex).grabFocus();
	    						}
								sudokuSolver.setIndexValue(SudokuSolver.INDEXES[sudokuIndexTextFieldList.indexOf(sudokuIndexTextField)]
										, Integer.parseInt(sudokuIndexTextField.getText()));
	                        }
	                	}
	                	
	                	@Override
	                	public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
	                		super.remove(fb, offset, length);
	                		if(!updatingList){
								sudokuSolver.setIndexValue(SudokuSolver.INDEXES[sudokuIndexTextFieldList.indexOf(sudokuIndexTextField)]
										, 0);
	                		}
	                	}
	                });
			sudokuIndexTextField.setHorizontalAlignment(JTextField.CENTER);
			sudokuIndexTextField.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					sudokuIndexTextField.setBackground(Color.WHITE);
					sudokuIndexTextField.setCaretColor(sudokuIndexTextField.getBackground());
				}
				
				@Override
				public void focusGained(FocusEvent e) {
					resetIndexesBackgroundColor();
					sudokuIndexTextField.setBackground(Color.cyan);
					sudokuIndexTextField.setCaretColor(sudokuIndexTextField.getBackground());
					if(sudokuIndexTextField.getText().length() == 1){
						sudokuIndexTextField.setCaretPosition(1);
					}
				}
			});
			sudokuIndexTextField.setCaretColor(sudokuIndexTextField.getBackground());
			sudokuIndexTextField.setCursor(new Cursor(Cursor.HAND_CURSOR));
			sudokuIndexTextField.setFont(font);
			subSudokuGridJPanel.add(sudokuIndexTextField);
		}
		return subSudokuGridJPanel;
	}
	
	/**
	 * 
	 */
	private void resetIndexesBackgroundColor(){
		if(sudokuIndexTextFieldList != null && !sudokuIndexTextFieldList.isEmpty()){
			for(JTextField jTextField : sudokuIndexTextFieldList){
				jTextField.setBackground(Color.WHITE);
			}
		}
	}

	@Override
	public void displaySudoku(String oneLineSudoku) {
		resetIndexesBackgroundColor();
		updatingList = true;
		for(int i = 0; i < 81; i++){
			char sudokuValue = oneLineSudoku.charAt(i);
			if(sudokuValue == '.'){
				sudokuIndexTextFieldList.get(i).setText("");
			} else {
				sudokuIndexTextFieldList.get(i).setText("" + sudokuValue);
			}
		}
		updatingList = false;
	}

	@Override
	public void showSolveTime(long solveTimeNs) {
		solveTimeLabel.setText("Solve time: " + TimeUnit.MILLISECONDS.convert(solveTimeNs, TimeUnit.NANOSECONDS) + "ms");
	}

	@Override
	public void printMsg(String msg) {
		logTextArea.append(new Date() + ": " + msg + "\n");
	}

	@Override
	public void printErr(String err) {
		logTextArea.append(new Date() + ": " +  err + "\n");
	}

	@Override
	public void showHint(String hintIndex) {
		resetIndexesBackgroundColor();
		for(int i = 0; i < 81; i++){
			if(SudokuSolver.INDEXES[i].equals(hintIndex)){
				sudokuIndexTextFieldList.get(i).setBackground(Color.GREEN);
			}
		}
	}

	@Override
	public void showInvalid(String invalidIndex) {
		for(int i = 0; i < 81; i++){
			if(SudokuSolver.INDEXES[i].equals(invalidIndex)){
				sudokuIndexTextFieldList.get(i).setBackground(Color.RED);
			}
		}		
	}

}
