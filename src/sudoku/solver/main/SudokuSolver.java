package sudoku.solver.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SudokuSolver {
	
	private static final int SUDOKUSIZE = 81;
	private static final int MINVALUE = 1;
	private static final int MAXVALUE = 9;
	private static final int NOVALUE = 0;
	private static final int MINCLUES = 17;
	public static final String[] ROWLETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
	public static final Integer[] COLNUMBERS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
	public static final String[] INDEXES = {"A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9",
											"B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9",
											"C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9",
											"D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9",
											"E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9",
											"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9",
											"G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8", "G9",
											"H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9",
											"I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9",};
	
	public long solveTimeNs;
	private List<SolutionStep> solutionStepList;
	private List<SolutionStep> hintList;
	private SolutionStep activeHint;
	private Map<String, Integer> sudokuMap;
	private Map<String, Integer> initialSudokuMap;
	
	private SudokuDisplayer sudokuDisplayer;
	/* GRID MAP
	   	A1 A2 A3| A4 A5 A6| A7 A8 A9    
 		B1 B2 B3| B4 B5 B6| B7 B8 B9    
 		C1 C2 C3| C4 C5 C6| C7 C8 C9     
		---------*---------*---------    
	 	D1 D2 D3| D4 D5 D6| D7 D8 D9    
	 	E1 E2 E3| E4 E5 E6| E7 E8 E9   
	 	F1 F2 F3| F4 F5 F6| F7 F8 F9    
		---------*---------*---------   
	 	G1 G2 G3| G4 G5 G6| G7 G8 G9   
	 	H1 H2 H3| H4 H5 H6| H7 H8 H9    
	 	I1 I2 I3| I4 I5 I6| I7 I8 I9
	 */
	
	public interface SudokuDisplayer {
		void displaySudoku(String oneLineSudoku);
		void showSolveTime(long solveTimeNs);
		void printMsg(String msg);
		void printErr(String err);
		void showHint(String hintIndex);
		void showInvalid(String invalidIndex);
	}
	
	private class SudokuDummyDisplayer implements SudokuDisplayer{
		@Override
		public void displaySudoku(String oneLineSudoku) {}
		@Override
		public void showSolveTime(long solveTimeNs) {}
		@Override
		public void printMsg(String msg) {}
		@Override
		public void printErr(String err) {}
		@Override
		public void showHint(String hintIndex) {}
		@Override
		public void showInvalid(String invalidIndex) {}
	}
	
	private static class SolutionStep {
		String index;
		int value;
		SOLUTIONTYPE solutionType; 
		
		public SolutionStep(String index, int value, SOLUTIONTYPE solutionType) {
			this.index = index;
			this.value = value;
			this.solutionType = solutionType;
		}
	}

	enum SOLUTIONTYPE {
		NAKEDSINGLE,
		HIDDENSINGLE
	}
	
	public SudokuSolver() {
		this.sudokuDisplayer = new SudokuDummyDisplayer();
		load(getEmptySudokuMap());
	}
	
	/**
	 * 
	 * @param sudokuDisplayer
	 */
	public void setSudokuSolverDisplayer(SudokuDisplayer sudokuDisplayer){
		this.sudokuDisplayer = sudokuDisplayer;
	}
	
	/**
	 * Seeds a new Sudoku.
	 * @param oneLineSudoku
	 */
	public void seed(String oneLineSudoku) {
		load(convertOneLineSudokuToMap(oneLineSudoku));
	}
	
	/**
	 * Solves Sudoku and displays solve time.
	 */
	public void solve(){
		if(isSolveable()){
			long timeBeforeSolveNs = System.nanoTime();
			Map<String, Integer> sudokuSolutionMap = getSudokuSolutionMap(sudokuMap, solutionStepList);
			if(!sudokuSolutionMap.isEmpty()){
				solveTimeNs = System.nanoTime() - timeBeforeSolveNs;
				sudokuMap.clear();
				sudokuMap.putAll(sudokuSolutionMap);
				hintList.clear();
				sudokuDisplayer.showSolveTime(solveTimeNs);
				printSolutionStepList();
			} else {
				sudokuDisplayer.printErr("No solution found");
			}
		}
		sudokuDisplayer.displaySudoku(convertSudokuMapToOneLineString(sudokuMap));
	}
	
	/**
	 * Resets Sudoku to initially loaded/seeded state.
	 */
	public void reset(){
		sudokuMap.clear();
		sudokuMap.putAll(initialSudokuMap);
		solutionStepList.clear();
		hintList.clear();
		solveTimeNs = 0l;
		sudokuDisplayer.showSolveTime(solveTimeNs);
		sudokuDisplayer.displaySudoku(convertSudokuMapToOneLineString(sudokuMap));
	}
	
	/**
	 * Shows solution temporarily.
	 */
	public void showSolution(){
		if(isSolveable()){
			Map<String, Integer> sudokuSolutionMap = getSudokuSolutionMap(sudokuMap, new ArrayList<SolutionStep>());
			if(!sudokuSolutionMap.isEmpty()){
				sudokuDisplayer.displaySudoku(convertSudokuMapToOneLineString(sudokuSolutionMap));
			} else {
				sudokuDisplayer.printErr("No solution found");
			}
		}
	}
	
	/**
	 * Hides temporarily show solution.
	 */
	public void hideSolution(){
		boolean isSudokuFilled = isSudokuFilled();
		if(isSudokuFilled){
			sudokuDisplayer.printErr("Sudoku is already filled");
		}
		if(!isSudokuFilled){
			sudokuDisplayer.displaySudoku(convertSudokuMapToOneLineString(sudokuMap));
		}
	}
	
	/**
	 * Inserts a new value for index.
	 * @param editedIndex Index to insert value for.
	 * @param value Value to insert.
	 */
	public void setIndexValue(String editedIndex, int value){
		for(String index : INDEXES){
			if(editedIndex.equals(index)){
				sudokuMap.put(editedIndex, value);
				hintList.clear();
				return;
			}
		}
		sudokuDisplayer.printErr("Edited index " + editedIndex + " does not exist");
	}
	
	/**
	 * Shows a hint and keeps track of last shown hint to display next hint.
	 */
	public void showHint(){
		if(isSolveable()){
			if(hintList.isEmpty()){
				Map<String, Integer> sudokuSolutionMap = getSudokuSolutionMap(sudokuMap, hintList);
				if(sudokuSolutionMap.isEmpty()){
					hintList.clear();
					sudokuDisplayer.printErr("Cannot display hint (No solution to sudoku)");
				}
				activeHint = null;
			}
			if(hintList.size() > 0){
				if(activeHint == null){
					activeHint = hintList.get(0);
				} else {
					int nextHintIndex = hintList.indexOf(activeHint) + 1;
					if(nextHintIndex < hintList.size()){
						activeHint = hintList.get(nextHintIndex);
					} else {
						activeHint = hintList.get(0);
					}
				}
				sudokuDisplayer.showHint(activeHint.index);
				sudokuDisplayer.printMsg(activeHint.solutionType + " in cell " + activeHint.index);
			} else {
				sudokuDisplayer.printErr("Cannot display hint (No hints available)");
			}
		}
	}
	
	/**
	 * Checks if values on Sudoku are valid by Sudoku rules.
	 */
	public void checkValid(){
		for(String index : sudokuMap.keySet()){
			int value = sudokuMap.get(index);
			if(value != NOVALUE && !isValid(sudokuMap, index, value)){
				sudokuDisplayer.showInvalid(index);
				sudokuDisplayer.printMsg(index + " is not valid by sudoku rules");
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static Map<String, Integer> getEmptySudokuMap(){
		LinkedHashMap<String, Integer> emptySudokuMap = new LinkedHashMap<String, Integer>(SUDOKUSIZE);
		for(String rowLetter : ROWLETTERS){
			for(Integer colNr : COLNUMBERS){
				emptySudokuMap.put(rowLetter + colNr, NOVALUE);
			}
		}
		return emptySudokuMap;
	}
	
	// private stuff
	
	/**
	 * 
	 */
	private void printSolutionStepList(){
		for(SolutionStep solutionStep : solutionStepList){
			sudokuDisplayer.printMsg(solutionStep.solutionType + " in cell " + solutionStep.index + " with value " + solutionStep.value);
		}
	}
	
	/**
	 * Checks if Sudoku has necessary requirements to be solved.
	 * @return
	 */
	private boolean isSolveable(){
		boolean isSudokuFilled = isSudokuFilled();
		if(isSudokuFilled){
			sudokuDisplayer.printErr("Sudoku is already filled");
		}
		boolean isSudokuValid = isSudokuValid();
		if(!isSudokuValid){
			sudokuDisplayer.printErr("Sudoku is not valid");
		}
		boolean hasMinClues = hasMinClues();
		if(!hasMinClues){
			sudokuDisplayer.printErr("Sudoku does not have at least " + MINCLUES + " clues");
		}
		if(!isSudokuFilled && isSudokuValid && hasMinClues){
			return true;
		}
		return false;
	}
	
	/**
	 * Loads a new Sudoku.
	 * @param newSudokuMap Sudoku map.
	 */
	private void load(Map<String, Integer> newSudokuMap){
		initialSudokuMap = new LinkedHashMap<String, Integer>(newSudokuMap);
		sudokuMap = new LinkedHashMap<String, Integer>(newSudokuMap);
		solutionStepList = new ArrayList<SolutionStep>();
		hintList = new ArrayList<SolutionStep>();
		solveTimeNs = 0l;
		sudokuDisplayer.displaySudoku(convertSudokuMapToOneLineString(initialSudokuMap));
	}
	
	/**
	 * 
	 * @param oneLineSudoku
	 * @return
	 */
	private Map<String, Integer> convertOneLineSudokuToMap(String oneLineSudoku){
		if(oneLineSudoku.length() != SUDOKUSIZE){
			sudokuDisplayer.printErr("Character string is not 81 characters long");
			return getEmptySudokuMap();
		}
		for(int i = 0; i < oneLineSudoku.length(); i++){
			char c = oneLineSudoku.charAt(i);
			if(c != '.'
					&& !Character.isDigit(c)){
				sudokuDisplayer.printErr("Character string contains invalid characters: " + c);
				return getEmptySudokuMap();
			}
		}
		Map<String, Integer> sudokuMap = new LinkedHashMap<String, Integer>(SUDOKUSIZE);
		for(int i = 0; i < oneLineSudoku.length(); i++){
			char c = oneLineSudoku.charAt(i);
			if(c == '.'){
				sudokuMap.put(INDEXES[i], NOVALUE);
			} else {
				sudokuMap.put(INDEXES[i], Character.getNumericValue(c));
			}
		}
		return sudokuMap;
	}
	
	/**
	 * Checks if Sudoku is valid by sudoku rules.
	 * @param sudokuMap Sudoku map.
	 * @return True if valid else false.
	 */
	private boolean isSudokuValid(){
		for(String index : sudokuMap.keySet()){
			int value = sudokuMap.get(index);
			if(value != NOVALUE && !isValid(sudokuMap, index, value)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if Sudoku is filled (has no indexes without values).
	 * @param sudokuMap Sudoku map.
	 * @return True if filled else false.
	 */
	private boolean isSudokuFilled(){
		for(String index : sudokuMap.keySet()){
			int value = sudokuMap.get(index);
			if(value == NOVALUE){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if Sudoku has minimum amount of clues.
	 * @param sudokuMap Sudoku map.
	 * @return True if has min amount of clues else false.
	 */
	private boolean hasMinClues(){
		int clueCounter = 0;
		for(String index : sudokuMap.keySet()){
			int value = sudokuMap.get(index);
			if(value != NOVALUE){
				clueCounter++;
			}
		}
		if(clueCounter < MINCLUES){
			return false;
		}
		return true;
	}
	
	/**
	 * Gets a solution for a Sudoku.
	 * @param sudokuMap Sudoku map.
	 * @param solutionStepList Keeps track of solving methods used.
	 * @return Solution for sudoku otherwise empty map.
	 */
	private static Map<String, Integer> getSudokuSolutionMap(Map<String, Integer> sudokuMap, List<SolutionStep> solutionStepList){
		ArrayList<Map<String, Integer>> solutionList = new ArrayList<Map<String,Integer>>();
		Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap = getAllNoValueIndexCandidatesMap(sudokuMap);
		Map<String, Integer> bufferMap = new LinkedHashMap<String, Integer>(sudokuMap);
		fillNakedSingles(bufferMap, allNoValueIndexCandidatesMap, solutionStepList);
		fillHiddenSingles(bufferMap, allNoValueIndexCandidatesMap, solutionStepList);
		Map<String, Integer> sortedByCandidateListSizeMap = getSortedByCandidateListSizeMap(allNoValueIndexCandidatesMap, bufferMap);
		if(sortedByCandidateListSizeMap.isEmpty()){
			solutionList.add(new LinkedHashMap<String, Integer>(bufferMap));
		} else {
			backtrack(sortedByCandidateListSizeMap, solutionList);
		}
		if(solutionList.size() == 1){
			return solutionList.get(0);
		}
		return new HashMap<String, Integer>();
	}
	
	/**
	 * Finds no value index with the least amount of candidates and puts it first in the map.
	 * Adds all other no value indexes and then value indexes to the list in described order.
	 * All of it is done in default layout.
	 * @param allNoValueIndexCandidatesMap Map of all no value indexes.
	 * @param sudokuMap Sudoku map.
	 * @return A list where no value index with least amount of candidate is first, followed by no value indexes and value indexes.
	 */
	private static Map<String, Integer> getSortedByCandidateListSizeMap(Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap, Map<String, Integer> sudokuMap){
		Map<String, Integer> sortedByCandidateListSizeMap = new LinkedHashMap<String, Integer>(SUDOKUSIZE);
		String firstIndex = null;
		for(int i = MINVALUE; i <= MAXVALUE; i++){
			for(String index : allNoValueIndexCandidatesMap.keySet()){
				if(firstIndex == null &&
						allNoValueIndexCandidatesMap.get(index).size() == i){
					sortedByCandidateListSizeMap.put(index, NOVALUE);
					firstIndex = index;
					break;
				}
			}
			if(firstIndex != null){
				break;
			}
		}
		if(firstIndex != null){
			for(String index : allNoValueIndexCandidatesMap.keySet()){
				if(!firstIndex.equals(index)){
					sortedByCandidateListSizeMap.put(index, NOVALUE);
				}
			}
			
			for(String index : sudokuMap.keySet()){
				Integer value = sudokuMap.get(index);
				if(value != NOVALUE){
					sortedByCandidateListSizeMap.put(index, value);
				}
			}
		}
		return sortedByCandidateListSizeMap;
	}
	
	/**
	 * Finds all possible hidden singles.
	 * @param sudokuMap Sudoku map.
	 * @param allNoValueIndexCandidatesMap Map of all no value indexes.
	 * @param solutionStepList Steps used for finding solution.
	 */
	private static void fillHiddenSingles(Map<String, Integer> sudokuMap, Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap,  List<SolutionStep> solutionStepList){
		int hiddenSinglesFound = 0;
		do {
			hiddenSinglesFound = 0;
			for(String index : allNoValueIndexCandidatesMap.keySet()){
				ArrayList<Integer> candidateList = allNoValueIndexCandidatesMap.get(index);
				Integer hiddenSingle = getHiddenSingle(sudokuMap, candidateList, index);
				if(hiddenSingle != NOVALUE){
					hiddenSinglesFound++;
					sudokuMap.put(index, hiddenSingle);
					solutionStepList.add(new SolutionStep(index, hiddenSingle, SOLUTIONTYPE.HIDDENSINGLE));
					candidateList.clear();
					removeCandidateFromPeers(sudokuMap, solutionStepList, allNoValueIndexCandidatesMap, index, hiddenSingle, false);
				}
			}
		} while(hiddenSinglesFound > 0);
	}
	
	/**
	 * Gets a hidden single, if not found returns NOVALUE
	 * @param sudokuMap Sudoku map.
	 * @param candidateList Candidates to try for a hidden single.
	 * @param index No value index to find peers for and compare to candidates.
	 * @return
	 */
	private static Integer getHiddenSingle(Map<String, Integer> sudokuMap, ArrayList<Integer> candidateList, String index){
		String indexRowLetter = getRowLetter(index);
		int indexColNr = getColNr(index);
		ArrayList<Integer> rowNoValueIndexCandidateList = getMultipleNoValueIndexCandidateList(sudokuMap, getRowPeersIndexList(indexRowLetter, indexColNr));
		Integer hiddenSingle = getHiddenSingle(candidateList, rowNoValueIndexCandidateList);
		if(hiddenSingle == NOVALUE){
			ArrayList<Integer> colNoValueIndexCandidateList = getMultipleNoValueIndexCandidateList(sudokuMap, getColPeersIndexList(indexRowLetter, indexColNr));
			hiddenSingle = getHiddenSingle(candidateList, colNoValueIndexCandidateList);
			if(hiddenSingle == NOVALUE){
				ArrayList<Integer> regionNoValueIndexCandidateList = getMultipleNoValueIndexCandidateList(sudokuMap, getRegionPeersIndexList(indexRowLetter, indexColNr));
				hiddenSingle = getHiddenSingle(candidateList, regionNoValueIndexCandidateList);
			}
		}
		return hiddenSingle;
	}
	
	/**
	 * Gets a hidden single, if not found returns NOVALUE
	 * @param candidateList Candidates to try for a hidden single.
	 * @param multipleNoValueIndexCandidateList Peers to compare candidate to to determine if it is hidden single.
	 * @return
	 */
	private static Integer getHiddenSingle(ArrayList<Integer> candidateList, ArrayList<Integer> multipleNoValueIndexCandidateList){
		for(Integer candidate : candidateList){
			if(!multipleNoValueIndexCandidateList.contains(candidate)){
				return candidate;
			}
		}
		return NOVALUE;
	}
	
	/**
	 * Finds all possible naked singles.
	 * @param sudokuMap Sudoku map.
	 * @param allNoValueIndexCandidatesMap Map of all no value indexes.
	 * @param solutionStepList Steps used for finding solution.
	 */
	private static void fillNakedSingles(Map<String, Integer> sudokuMap, Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap, List<SolutionStep> solutionStepList){
		int nakedSinglesFound = 0;
		do {
			nakedSinglesFound = 0;
			for(String index : allNoValueIndexCandidatesMap.keySet()){
				ArrayList<Integer> candidateList = allNoValueIndexCandidatesMap.get(index);
				if(candidateList.size() == 1){
					nakedSinglesFound++;
					Integer nakedSingle = candidateList.get(0);
					sudokuMap.put(index, nakedSingle);
					solutionStepList.add(new SolutionStep(index, nakedSingle, SOLUTIONTYPE.NAKEDSINGLE));
					candidateList.clear();
					removeCandidateFromPeers(sudokuMap, solutionStepList, allNoValueIndexCandidatesMap, index, nakedSingle, true);
				}
			}
		} while(nakedSinglesFound > 0);
	}
	
	/**
	 * Removes candidate from index peers.
	 * @param sudokuMap Sudoku map.
	 * @param solutionStepList Steps used for finding solution.
	 * @param allNoValueIndexCandidatesMap Map of all no value indexes.
	 * @param index Index to get peers for.
	 * @param candidate Candidate to be removed.
	 * @param fillingNakedSingles True if called when filling naked singles.
	 */
	private static void removeCandidateFromPeers(Map<String, Integer> sudokuMap, List<SolutionStep> solutionStepList, Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap, String index, int candidate, boolean fillingNakedSingles){
		String indexRowLetter = getRowLetter(index);
		int indexColNr = getColNr(index);
		removeCandidate(sudokuMap, solutionStepList, allNoValueIndexCandidatesMap, getColPeersIndexList(indexRowLetter, indexColNr), candidate, fillingNakedSingles);
		removeCandidate(sudokuMap, solutionStepList, allNoValueIndexCandidatesMap, getRowPeersIndexList(indexRowLetter, indexColNr), candidate, fillingNakedSingles);
		removeCandidate(sudokuMap, solutionStepList, allNoValueIndexCandidatesMap, getUncheckedRegionPeersIndexList(indexRowLetter, indexColNr), candidate, fillingNakedSingles);
	}
	
	/**
	 * Removes a candidate from no value indexes and starts filling naked singles when possible.
	 * @param solutionStepList Steps used for finding solution.
	 * @param allNoValueIndexCandidatesMap Map of all no value indexes.
	 * @param indexList No value indexes to remove candidate from.
	 * @param candidate Candidate to be removed.
	 * @param fillingNakedSingles True if called when filling naked singles.
	 */
	private static void removeCandidate(Map<String, Integer> sudokuMap, List<SolutionStep> solutionStepList, Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap, ArrayList<String> indexList, Integer candidate, boolean fillingNakedSingles){
		for(String index : indexList){
			ArrayList<Integer> candidateList = allNoValueIndexCandidatesMap.get(index);
			if(candidateList != null){
				candidateList.remove(candidate);
				if(!fillingNakedSingles &&
						candidateList.size() == 1){
					fillNakedSingles(sudokuMap, allNoValueIndexCandidatesMap, solutionStepList);
				}
			}
		}
	}
	
	/**
	 * Gets a map of all no value indexes as keys and candidate list as value
	 * @param sudokuMap Sudoku map.
	 * @return Map of all no value indexes with candidates.
	 */
	private static Map<String, ArrayList<Integer>> getAllNoValueIndexCandidatesMap(Map<String, Integer> sudokuMap){
		Map<String, ArrayList<Integer>> allNoValueIndexCandidatesMap = new LinkedHashMap<String, ArrayList<Integer>>();
		for(String index : sudokuMap.keySet()){
			ArrayList<Integer> noValueIndexCandidateList = getNoValueIndexCandidateList(sudokuMap, index);
			if(!noValueIndexCandidateList.isEmpty()){
				allNoValueIndexCandidatesMap.put(index, noValueIndexCandidateList);
			}
		}
		return allNoValueIndexCandidatesMap;
	}
	
	/**
	 * Backtracking algorithm (brute-force).
	 * @param sudokuMap Sudoku map.
	 * @param solutionList List of solutions to sudoku.
	 */
	private static void backtrack(Map<String, Integer> sudokuMap, ArrayList<Map<String, Integer>> solutionList){
		for(String index : sudokuMap.keySet()){
			if(sudokuMap.get(index) == NOVALUE){
				for(int i = MINVALUE; i <= MAXVALUE; i++){
					if(isValid(sudokuMap, index, i)){
						sudokuMap.put(index, i);
						backtrack(sudokuMap, solutionList);
						if(solutionList.size() == 1){
							return;
						}
						sudokuMap.put(index, NOVALUE);
					}
				}
				return;
			}
		}
		solutionList.add(new LinkedHashMap<String, Integer>(sudokuMap));
	}
	
	/**
	 * Checks if number exists only once in its column.
	 * @param sudokuMap Sudoku map.
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @param value Value to be checked.
	 * @return True if @param value already exists in column.
	 */
	private static boolean isInCol(Map<String, Integer> sudokuMap, String indexRowLetter, int indexColNr, int value){
		for(Integer colNr : COLNUMBERS){
			if(colNr != indexColNr && sudokuMap.get(indexRowLetter + colNr) == value){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if number exists only once in its row.
	 * @param sudokuMap Sudoku map.
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @param value Value to be checked.
	 * @return True if @param value already exists in row.
	 */
	private static boolean isInRow(Map<String, Integer> sudokuMap, String indexRowLetter, int indexColNr, int value){
		for(String rowLetter : ROWLETTERS){
			if(!rowLetter.equals(indexRowLetter) && sudokuMap.get(rowLetter + indexColNr) == value){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if number exists only once in its region.
	 * @param sudokuMap Sudoku map.
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @param value Value to be checked.
	 * @return True if @param value already exists in region.
	 */
	private static boolean isInRegion(Map<String, Integer> sudokuMap, String indexRowLetter, int indexColNr, int value){
		ArrayList<String> uncheckedRegionPeersIndexList = getUncheckedRegionPeersIndexList(indexRowLetter, indexColNr);
		for(String uncheckedRegionPeerIndex : uncheckedRegionPeersIndexList){
			if(sudokuMap.get(uncheckedRegionPeerIndex) == value){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if number is valid by sudoku rules (exists only once in its row, column and region).
	 * @param sudokuMap Sudoku map.
	 * @param index Index to be checked.
	 * @param value Value to be checked.
	 * @return True if valid else false.
	 */
	private static boolean isValid(Map<String, Integer> sudokuMap, String index, int value){
		String indexRowLetter = getRowLetter(index);
		int indexColNr = getColNr(index);
		if(isInCol(sudokuMap, indexRowLetter, indexColNr, value) 
			|| isInRow(sudokuMap, indexRowLetter, indexColNr, value) 
			|| isInRegion(sudokuMap, indexRowLetter, indexColNr, value)){
			return false;
		}
		return true;
	}
	
	/**
	 * Gets region peers indexes that did/do not get checked if row and column checked beforehand.
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @return Indexes in region that did not get checked.
	 */
	private static ArrayList<String> getUncheckedRegionPeersIndexList(String indexRowLetter, int indexColNr){
		ArrayList<String> uncheckedRegionPeersIndexList = new ArrayList<String>(4);
		String[] regionRowLetters = getRegionRowLetters(indexRowLetter);
		int[] regionColNumbers = getRegionColNumbers(indexColNr);
		for(String rowLetter : regionRowLetters){
			for(int colNr : regionColNumbers){
				if(!rowLetter.equals(indexRowLetter) && colNr != indexColNr){
					uncheckedRegionPeersIndexList.add(rowLetter + colNr);
				}
			}
		}
		return uncheckedRegionPeersIndexList;
	}
	
	/**
	 * 
	 * @param indexRowLetter
	 * @return Region row letters.
	 */
	private static String[] getRegionRowLetters(String indexRowLetter){
		if(indexRowLetter.compareTo("D") < 0){
			return new String[] {"A", "B", "C"};
		} else if(indexRowLetter.compareTo("F") > 0){
			return new String[] {"G", "H", "I"};
		} else {
			return new String[] {"D", "E", "F"};
		}
	}
	
	/**
	 * 
	 * @param indexColNr
	 * @return Region column numbers.
	 */
	private static int[] getRegionColNumbers(int indexColNr){
		if(indexColNr < 4){
			return new int[]{1, 2, 3};
		} else if(indexColNr > 6){
			return new int[]{7, 8, 9};
		} else {
			return new int[]{4, 5, 6};
		}
	}
	
	/**
	 * 
	 * @param index
	 * @return Index letter (row letter).
	 */
	private static String getRowLetter(String index){
		return index.substring(0, 1);
	}
	
	/**
	 * 
	 * @param index
	 * @return Index number (column number).
	 */
	private static int getColNr(String index){
		return Integer.parseInt(index.substring(1, 2));
	}
	
	/**
	 * Gets all candidates for @param noValueIndexList.
	 * @param sudokuMap Sudoku map.
	 * @param noValueIndexList Indexes to find candidates for.
	 * @return List of candidates found.
	 */
	private static ArrayList<Integer> getMultipleNoValueIndexCandidateList(Map<String, Integer> sudokuMap, ArrayList<String> noValueIndexList){
		ArrayList<Integer> candidateList = new ArrayList<Integer>();
		for(String index : noValueIndexList){
			ArrayList<Integer> noValueIndexCandidateList = getNoValueIndexCandidateList(sudokuMap, index);
			if(!noValueIndexCandidateList.isEmpty()){
				candidateList.addAll(noValueIndexCandidateList);
			}
		}
		return candidateList;
	}
	
	/**
	 * Gets candidates for an index with no value.
	 * @param sudokuMap Sudoku map.
	 * @param noValueIndex No value index to get candidates for.
	 * @return Candidates for the index if it has no value otherwise empty list.
	 */
	private static ArrayList<Integer> getNoValueIndexCandidateList(Map<String, Integer> sudokuMap, String noValueIndex){
		ArrayList<Integer> candidateList = new ArrayList<Integer>();
		if(sudokuMap.get(noValueIndex) == NOVALUE){
			for(int i = MINVALUE; i <= MAXVALUE; i++){
				if(isValid(sudokuMap, noValueIndex, i)){
					candidateList.add(i);
				}
			}
		}
		return candidateList;
	}

	/**
	 * Gets indexes in the same column with the index combined from parameters.
	 * <p>
	 * Example: if @param indexRowLetter equals A and @param indexColNr equals 1
	 * then it gets indexes: B1, C1, D1, E1, F1, G1, H1, I1.
	 * </p>
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @return Indexes that are in the same column with index combined from parameters EXCLUDING the index combined from parameters.
	 */
	private static ArrayList<String> getColPeersIndexList(String indexRowLetter, int indexColNr){
		ArrayList<String> colPeersIndexList = new ArrayList<String>(8);
		for(String rowLetter : ROWLETTERS){
			if(!rowLetter.equals(indexRowLetter)){
				colPeersIndexList.add(rowLetter + indexColNr);
			}
		}
		return colPeersIndexList;
	}
	
	/**
	 * Gets indexes in the same row with the index combined from parameters.
	 * <p>
	 * Example: if @param indexRowLetter equals A and @param indexColNr equals 1
	 * then it gets indexes: A2, A3, A4, A5, A6, A7, A8, A9.
	 * </p>
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @return Indexes that are in the same row with index combined from parameters EXCLUDING the index combined from parameters.
	 */
	private static ArrayList<String> getRowPeersIndexList(String indexRowLetter, int indexColNr){
		ArrayList<String> rowPeersIndexList = new ArrayList<String>(8);
		for(Integer colNr : COLNUMBERS){
			if(colNr != indexColNr){
				rowPeersIndexList.add(indexRowLetter + colNr);
			}
		}
		return rowPeersIndexList;
	}
	
	/**
	 * Gets indexes in the same region with the index combined from parameters.
	 * <p>
	 * Example: if @param indexRowLetter equals A and @param indexColNr equals 1
	 * then it gets indexes: A2, A3, B1, B2, B3, C1, C2, C3.
	 * </p>
	 * @param indexRowLetter Row letter of index.
	 * @param indexColNr Column number of index.
	 * @return Indexes that are in the same region with index combined from parameters EXCLUDING the index combined from parameters.
	 */
	private static ArrayList<String> getRegionPeersIndexList(String indexRowLetter, int indexColNr){
		ArrayList<String> regionPeersIndexList = new ArrayList<String>(8);
		String[] regionRowLetters = getRegionRowLetters(indexRowLetter);
		int[] regionColNumbers = getRegionColNumbers(indexColNr);
		for(String rowLetter : regionRowLetters){
			for(int colNr : regionColNumbers){
				if(!(rowLetter.equals(indexRowLetter) && colNr == indexColNr))
				regionPeersIndexList.add(rowLetter + colNr);
			}
		}
		return regionPeersIndexList;
	}
	
	/**
	 * 
	 * @param sudokuMap
	 * @return
	 */
	private static String convertSudokuMapToOneLineString(Map<String, Integer> sudokuMap){
		String oneLineSudoku = "";
		for(String index : INDEXES){
			int value = sudokuMap.get(index);
			if(value == NOVALUE){
				oneLineSudoku = oneLineSudoku.concat(".");
			} else {
				oneLineSudoku = oneLineSudoku.concat("" + value);
			}
		}
		return oneLineSudoku;
	}
}
