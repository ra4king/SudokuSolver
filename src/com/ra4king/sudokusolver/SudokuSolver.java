package com.ra4king.sudokusolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Roi (ra4king) Atalla
 */
public class SudokuSolver {
	public static void main(String[] args) {
		BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print("Input file: ");
		
		String fileName;
		try {
			fileName = stdinReader.readLine();
		} catch(Exception exc) {
			exc.printStackTrace();
			return;
		}
		
		if(fileName == null) {
			System.out.println("Strange error, input is null?");
			return;
		}
		
		int[][] puzzle = new int[9][9];
		
		try(BufferedReader fileReader = new BufferedReader(new FileReader(new File(fileName)))) {
			int row = 0;
			
			String line;
			while((line = fileReader.readLine()) != null) {
				int col = 0;
				for(char c : line.toCharArray()) {
					if(col == 9) {
						System.out.println("Invalid sudoku puzzle.");
						return;
					}
					
					if(c == ' ') {
						col++;
						continue;
					}
					
					int n = c - '0';
					
					if(n < 0 || n > 9)
						continue;
					
					puzzle[col++][row] = n;
				}
				
				row++;
			}
			
			if(row != 9) {
				System.out.println("Invalid sudoku puzzle.");
				return;
			}
		} catch(Exception exc) {
			exc.printStackTrace();
			return;
		}
		
		int dotIndex = fileName.lastIndexOf('.');
		String fileNameTrimmed = fileName.substring(0, dotIndex == -1 ? fileName.length() : dotIndex);
		String outputFileName;
		
		SudokuSolver solver = new SudokuSolver(puzzle);
		if(!solver.solve()) {
			System.out.println("Unable to solve sudoku. Saving current progress as " + fileNameTrimmed + "-unsolved.txt");
			outputFileName = fileNameTrimmed + "-unsolved.txt";
		}
		else {
			System.out.println("Sudoku solved. Saving as " + fileNameTrimmed + "-solved.txt");
			outputFileName = fileNameTrimmed + "-solved.txt";
		}
		
		try(FileWriter fileWriter = new FileWriter(new File(outputFileName))) {
			int[][] solved = solver.getPuzzle();
			
			fileWriter.write("+-----------------------+\n");
			for(int y = 0; y < 9; y++) {
				if(y > 0 && y % 3 == 0)
					fileWriter.write("|-------+-------+-------|\n");
				
				for(int x = 0; x < 9; x++) {
					if(x % 3 == 0)
						fileWriter.write("| ");
					
					if(solved[x][y] == 0)
						fileWriter.write(' ');
					else
						fileWriter.write('0' + solved[x][y]);
					
					fileWriter.write(' ');
				}
				fileWriter.write("|\n");
			}
			fileWriter.write("+-----------------------+");
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}
	
	// Cell[column][row]
	private Cell[][] puzzle;
	
	public SudokuSolver(int[][] puzzle) {
		if(puzzle.length != 9)
			throw new IllegalArgumentException("Invalid sudoku puzzle.");
		
		this.puzzle = new Cell[9][9];
		
		for(int x = 0; x < 9; x++) {
			if(puzzle[x].length != 9)
				throw new IllegalArgumentException("Invalid sudoku puzzle.");
			
			for(int y = 0; y < 9; y++)
				this.puzzle[x][y] = new Cell(puzzle[x][y]);
		}
	}
	
	// This makes a completely clone of each Cell object
	private Cell[][] clone(Cell[][] c) {
		Cell[][] n = new Cell[c.length][];
		
		for(int a = 0; a < c.length; a++) {
			n[a] = new Cell[c[a].length];
			
			for(int b = 0; b < c.length; b++)
				n[a][b] = new Cell(c[a][b]);
		}
		
		return n;
	}
	
	// This only copies the references from 'orig' to 'dest'
	private void copy(Cell[][] orig, Cell[][] dest) {
		if(orig.length != dest.length)
			throw new IllegalArgumentException("Not the same length.");
		
		for(int a = 0; a < orig.length; a++)
			dest[a] = Arrays.copyOf(orig[a], orig.length);
	}
	
	public boolean solve() {
		long now = System.nanoTime();
		boolean result = solve(puzzle);
		System.out.println("Solving took " + (System.nanoTime() - now) / 1e6f + " milliseconds.");
		return result;
	}
	
	// Returns true if the puzzle was solved, false otherwise
	private boolean solve(Cell[][] puzzle) {
		// loops through until unsolvedCount is 0 or isn't decreasing
		int unsolvedCount, prevUnsolvedCount = -1;
		do {
			unsolvedCount = 0;
			
			for(int a = 0; a < 81; a++) {
				int x = a % 9;
				int y = a / 9;
				
				if(puzzle[x][y].value == 0) {
					if(!solve(puzzle, x, y))
						unsolvedCount++;
				}
			}
			
			// no new solutions found if unsolved count is the same as last time
			if(unsolvedCount == prevUnsolvedCount) {
				for(int a = 0; a < 81; a++) {
					int x = a % 9;
					int y = a / 9;
					
					// find the first unsolved cell
					if(puzzle[x][y].value == 0) {
						// if there are possible solutions, test each
						while(puzzle[x][y].hasPossibleSolutions()) {
							// clone the puzzle, remove the first possible solution, and assigns it
							Cell[][] newPuzzle = clone(puzzle);
							newPuzzle[x][y].value = puzzle[x][y].possibleSolutions.remove(0);
							newPuzzle[x][y].possibleSolutions.clear();
							
							// recursive call! if solve returns false, then we loop back and try the next possible solution
							if(solve(newPuzzle)) {
								copy(newPuzzle, puzzle); // success! set the original puzzle to the solved and return
								return true;
							}
						}
						
						// all possible solutions for this cell failed
						return false;
					}
				}
			}
			
			prevUnsolvedCount = unsolvedCount;
		} while(unsolvedCount > 0);
		
		return true;
	}
	
	private boolean solve(Cell[][] puzzle, int x, int y) {
		Cell cell = puzzle[x][y];
		
		if(cell.value != 0)
			return true;
		
		if(cell.hasPossibleSolutions()) {
			for(int a = cell.possibleSolutions.size() - 1; a >= 0; a--) {
				int n = cell.possibleSolutions.get(a);
				
				SolutionCase sc = isSolution(puzzle, x, y, n);
				if(sc == SolutionCase.NOT_SOLUTION)
					cell.possibleSolutions.remove(a);
				else if(sc == SolutionCase.DEFINITE_SOLUTION) {
					cell.value = n;
					cell.possibleSolutions.clear();
					return true;
				}
			}
		}
		else {
			for(int n = 1; n <= 9; n++) {
				SolutionCase sc = isSolution(puzzle, x, y, n);
				if(sc == SolutionCase.POSSIBLE_SOLUTION)
					cell.addPossibleSolution(n);
				else if(sc == SolutionCase.DEFINITE_SOLUTION) {
					cell.value = n;
					cell.possibleSolutions.clear();
					return true;
				}
			}
		}
		
		if(cell.possibleSolutions.size() == 1) {
			cell.value = cell.possibleSolutions.remove(0);
			return true;
		}
		
		return false;
	}
	
	private SolutionCase isSolution(Cell[][] puzzle, int x, int y, int value) {
		int rowFilledCount = 0, colFilledCount = 0;
		
		for(int i = 0; i < 9; i++) {
			if(puzzle[i][y].value == value)
				return SolutionCase.NOT_SOLUTION;
			if(puzzle[x][i].value == value)
				return SolutionCase.NOT_SOLUTION;
			
			if(puzzle[i][y].value != 0)
				rowFilledCount++;
			if(puzzle[x][i].value != 0)
				colFilledCount++;
		}
		
		int ix = x / 3;
		int iy = y / 3;
		
		int cellFilledCount = 0;
		
		for(int i = 0; i < 9; i++) {
			int cx = ix * 3 + i % 3;
			int cy = iy * 3 + i / 3;
			
			if(puzzle[cx][cy].value == value)
				return SolutionCase.NOT_SOLUTION;
			
			if(puzzle[cx][cy].value != 0)
				cellFilledCount++;
		}
		
		if(rowFilledCount == 8 || colFilledCount == 8 || cellFilledCount == 8)
			return SolutionCase.DEFINITE_SOLUTION;
		
		return SolutionCase.POSSIBLE_SOLUTION;
	}
	
	public int[][] getPuzzle() {
		int[][] puzzle = new int[9][9];
		for(int x = 0; x < 9; x++)
			for(int y = 0; y < 9; y++)
				puzzle[x][y] = this.puzzle[x][y].value;
		
		return puzzle;
	}
	
	private static enum SolutionCase {
		NOT_SOLUTION, POSSIBLE_SOLUTION, DEFINITE_SOLUTION
	}
	
	private static class Cell {
		private int value;
		private ArrayList<Integer> possibleSolutions;
		
		public Cell(int value) {
			this.value = value;
			possibleSolutions = new ArrayList<>();
		}
		
		public Cell(Cell c) {
			this(c.value);
			possibleSolutions.addAll(c.possibleSolutions);
		}
		
		public void addPossibleSolution(int value) {
			possibleSolutions.add(value);
		}
		
		public boolean hasPossibleSolutions() {
			return !possibleSolutions.isEmpty();
		}
	}
}
