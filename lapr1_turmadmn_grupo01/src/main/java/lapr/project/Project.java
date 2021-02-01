package lapr.project;

import org.la4j.Matrix;
import org.la4j.decomposition.EigenDecompositor;
import org.la4j.matrix.dense.Basic1DMatrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Project {
	
	static String pathGnuplot = "C:\\Program Files\\gnuplot\\bin\\gnuplot";
	static String dataNameFile = "Data.txt";
	static String plotNameFile = "Template.gp";
	static String outputDir = "outputs";
	
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        int force = 1;

		//Modo interativo sem ficheiro
		if (args.length == 0) {
			while(force == 1) {
				interactiveWayNonFile(in);
				
				force = -1;
				while (force == -1) {
					System.out.println("Deseja sair ou continuar no programa?");
					System.out.println(" 1-Continuar");
					System.out.println(" 0-Sair");
					force = in.nextInt();

					if (force < 0 || force > 1) {
						System.out.println("Escolha inválida.");
						force = -1;
					}
				}
			}
		}

		//Modo interativo com ficheiro: java -jar nome_programa.jar -n nome_ficheiro_entrada.txt
		if (args.length == 2) {
			if (order_class(args[args.length - 1])) {
				System.out.println("Ficheiro corretamente formatado.");
			}
			while(force == 1) {
				interactiveWayWithFile(in, args);

				force = -1;
				while (force == -1) {
					System.out.println("Deseja sair ou continuar no programa?");
					System.out.println(" 1-Continuar");
					System.out.println(" 0-Sair");
					force = in.nextInt();

					if (force < 0 || force > 1) {
						System.out.println("Escolha inválida.");
						force = -1;
					}
				}
			}
		}

        //Modo não interativo com ficheiro: java -jar nome_programa.jar -t XXX -g Y -e -v -r nome_ficheiro_entrada.txt nome_ficheiro_saida.txt
        if (args.length > 2) {
        	if (order_class(args[args.length - 2])) {
				nonInteractiveWay(args);
				
			}
        	else {
        		System.exit(0);
			}
        }
    }

    public static void interactiveWayNonFile(Scanner in) throws IOException {
    	int dim = -1;
        int gen = -1;
        int graph = -1;
        int save = -1;
        String fileName = "";
        String populationName = "";
        
    	do {
			System.out.println("Número de grupos etários (dimensão): ");
			dim = in.nextInt();
			System.out.println("Número de gerações a calcular: ");
			gen = in.nextInt();
		} while (dim < 0 && gen < 0);

		//Criar Matriz Leslie
		double[][] leslie = LeslieMatrix(dim);
		System.out.println("Matriz de Leslie: ");
		Matrix matrix_leslie = convertToMatrix(leslie);
		System.out.println(matrix_leslie);
		
		System.out.println("Nome da população: ");
		if (in.hasNextLine()) {
			in.nextLine();
		}
		populationName = in.nextLine();

		double eigenvalue = eigen_value(leslie);
		System.out.println("Valor Próprio: ");
		System.out.println(eigenvalue);

		double[] eigenvector = eigen_vec(leslie);
		System.out.println("Vetor Próprio: ");
		System.out.println(Arrays.toString(eigenvector));

		System.out.println("Valores da população Inicial: ");
		double[][] population = new double[dim][1];
		for (int i = 0; i < dim; i++) {
			population[i][0] = in.nextDouble();
		}

		double[] totalPopulationChange = new double[gen + 1];
		double value = -1;
		for (int i = 0; i <= gen; i++) {
			Matrix populationResult = dimPopulationinT(leslie, population, i);
			totalPopulationChange[i] = totaldimPopulation(populationResult);
			double[][]populationResult2 = convertToDouble(populationResult);
			value = totalPopulationChange[i];
			value = value * 100;
			value = Math.ceil(value);
			value = value / 100;
			System.out.println("Dimensão da população em t = " + i);
			for(int j = 0; j < dim; j++) {
				System.out.println(String.format("%.2f", populationResult2[j][0]));
			}
			System.out.println("Total da população em t = " + i);
			System.out.println(String.format("%.2f", value));
		}

		double[] rateOfChange = new double[gen];
		value = -1;
		rateOfChange = rateofchange(leslie, population, gen);
		System.out.println("Taxa de variação ao longo dos anos: ");
		for (int i = 0; i < gen; i++) {
			value = rateOfChange[i];
			value = value * 100;
			value = Math.ceil(value);
			value = value / 100;
			System.out.println(String.format("%.2f", value));
		}

		String txt = "";
		System.out.println("Valor de classes: ");
		double[][] numberOfClasses = new double[gen + 1][dim];
		for (int i = 0; i <= gen; i++) {
			numberOfClasses[i] = dimPopulationinT(leslie, population, i).getColumn(0).toDenseVector().toArray();
			txt = "[";
			for(int j = 0; j < numberOfClasses[i].length; j++) {
				if(j!=0) {
					txt += ", ";
				}
				txt += String.format("%.2f", numberOfClasses[i][j]);
			}
			txt += "]";
			System.out.println(txt);
		}

		double[][] graphResults = new double[1][1];
		String graphTitle = "";
		String resulType = "";
		String xLine = "";
		String yLine = "";
		int graphType = -1;
		graph = -1;
		while (graph == -1) {
			System.out.println("Qual dos gráficos deseja gerar: ");
			System.out.println(" 1-Número total de individuos");
			System.out.println(" 2-Crescimento da população");
			System.out.println(" 3-Numero por classe (não normalizado)");
			System.out.println(" 4-Numero por classe (normalizado)");
			graph = in.nextInt();

			switch (graph) {
				case 1:
					graphResults = new double[1][gen + 1];
					for (int i = 0; i < gen + 1; i++) {
						graphResults[0][i] = totalPopulationChange[i];
					}
					graphTitle = "Número Total De Individuos";
					resulType = "Número Total De Individuos";
					xLine = "Momento";
					yLine = "Dimensão da população";
					graphType = 1;
					break;
				case 2:
					graphResults = new double[1][gen];
					for (int i = 0; i < gen; i++) {
						graphResults[0][i] = rateOfChange[i];
					}
					graphTitle = "Crescimento da população";
					resulType = "Crescimento da população";
					xLine = "Momento";
					yLine = "Variação";
					graphType = 2;
					break;
				case 3:
					graphResults = new double[gen + 1][dim];
					for (int i = 0; i < gen + 1; i++) {
						for (int j = 0; j < dim; j++) {
							graphResults[i][j] = numberOfClasses[i][j];
						}
					}
					graphTitle = "Número por Classe (não normalizado)";
					resulType = "Número por Classe";
					xLine = "Momento";
					yLine = "Classe";
					graphType = 3;
					break;
				case 4:
					graphResults = new double[gen + 1][dim];
					for (int i = 0; i < gen + 1; i++) {
						double total = totalPopulationChange[i];
						for (int j = 0; j < dim; j++) {
							if (total == 0) {
								graphResults[i][j] = 0;
							} else {
								graphResults[i][j] = 100 * numberOfClasses[i][j] / total;
							}
						}
					}
					graphTitle = "Número por Classe (normalizado)";
					resulType = "Número por Classe";
					xLine = "Momento";
					yLine = "Classe";
					graphType = 4;
					break;
				default:
					System.out.println("Escolha inválida.");
					graph = -1;
					break;
			}
		}
		createGraph(graphResults, 0, graphTitle, resulType, xLine, yLine, "");
		
		save = -1;
		while (save == -1) {
			System.out.println("Guardar como: ");
			System.out.println(" 1-png");
			System.out.println(" 2-txt");
			System.out.println(" 3-eps");
			System.out.println(" 0-Não guardar");
			save = in.nextInt();

			if (save < 0 || save > 3) {
				System.out.println("Escolha inválida.");
				save = -1;
			}
		}
		if (save != 0) {
			System.out.println("Nome do ficheiro: ");
			if (in.hasNextLine()) {
				in.nextLine();
			}
			fileName = in.nextLine();

			String defaultExtension = "";
			switch (save) {
				case 1:
					defaultExtension = ".png";
					break;
				case 2:
					defaultExtension = ".txt";
					break;
				case 3:
					defaultExtension = ".eps";
					break;
			}
			if (!fileName.toLowerCase().endsWith(defaultExtension)) {
				fileName = fileName + defaultExtension;
			}
			String data = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
			createGraph(graphResults, save, graphTitle, resulType, xLine, yLine, populationName + data + fileName);
		}
		creatingTxtFileGraph(leslie, gen, totalPopulationChange, 
				rateOfChange, numberOfClasses, eigenvalue, eigenvector, true,
				true, true);
    }
    
    public static void interactiveWayWithFile(Scanner in, String[] args) throws IOException {
    	int dim = -1;
        int gen = -1;
        int graph = -1;
        int save = -1;
        String fileName = "";
        String populationName = "";
        
    	do {
			System.out.println("Número de grupos etários: "); //grupos etários - dimensão
			dim = getdimfromLeslieMatrixFile(args[1]);
			System.out.println(dim); //Imprime mas não pede
			System.out.println("\nNúmero de gerações a calcular: "); //gerações
			gen = in.nextInt();
		} while (dim < 0 && gen < 0);

		//Criar Matriz Leslie com ficheiro
		Matrix matrix_leslie = LeslieMatrixFile(args[1], dim);
		System.out.println(matrix_leslie);
		double[][] leslie = convertToDouble(matrix_leslie);

		//Mostrar Resultados obtidos
		File f = new File(args[args.length-1]);
		populationName = f.getName();
		if(populationName.toLowerCase().endsWith(".txt")) {
			populationName = populationName.substring(0, populationName.length()-4);
		}

		double eigenvalue = eigen_value(leslie);
		System.out.println("Valor Próprio: ");
		System.out.println(eigenvalue);

		double[] eigenvector = eigen_vec(leslie);
		System.out.println("\nVetor Próprio: ");
		System.out.println(Arrays.toString(eigenvector));

		System.out.println("\nValores da população Inicial: ");
		double[][] population = getPopulationfromFile(args[1], dim);
		for (int i = 0; i < dim; i++) {
			System.out.println(population[i][0]);
		}

		double[] totalPopulationChange = new double[gen + 1];
		double value = -1;
		for (int i = 0; i <= gen; i++) {
			Matrix populationResult = dimPopulationinT(leslie, population, i); //Possivelmente teremos de alterar em dimPopulationinT inicialização de populationinT
			totalPopulationChange[i] = totaldimPopulation(populationResult);
			double[][]populationResult2 = convertToDouble(populationResult);
			value = totalPopulationChange[i];
			value = value * 100;
			value = Math.ceil(value);
			value = value / 100;
			System.out.println("Dimensão da população em t = " + i);
			for(int j = 0; j < dim; j++) {
				System.out.println(String.format("%.2f", populationResult2[j][0]));
			}
			System.out.println("Total da população em t = " + i);
			System.out.println(String.format("%.2f", value));
		}

		double[] rateOfChange = new double[gen];
		value = -1;
		rateOfChange = rateofchange(leslie, population, gen);
		System.out.println("Taxa de variação ao longo dos anos: ");
		for (int i = 0; i < gen; i++) {
			value = rateOfChange[i];
			value = value * 100;
			value = Math.ceil(value);
			value = value / 100;
			System.out.println(String.format("%.2f", value));
		}

		String txt = "";
		System.out.println("Valor de classes: ");
		double[][] numberOfClasses = new double[gen + 1][dim];
		for (int i = 0; i <= gen; i++) {
			numberOfClasses[i] = dimPopulationinT(leslie, population, i).getColumn(0).toDenseVector().toArray();
			txt = "[";
			for(int j = 0; j < numberOfClasses[i].length; j++) {
				if(j!=0) {
					txt += ", ";
				}
				txt += String.format("%.2f", numberOfClasses[i][j]);
			}
			txt += "]";
			System.out.println(txt);
		}

		double[][] graphResults = new double[1][1];
		String graphTitle = "";
		String resulType = "";
		String xLine = "";
		String yLine = "";
		int graphType = -1;
		graph = -1;
		while (graph == -1) {
			System.out.println("\n\nQual dos gráficos deseja gerar: ");
			System.out.println(" 1-Número total de individuos");
			System.out.println(" 2-Crescimento da população");
			System.out.println(" 3-Numero por classe (não normalizado)");
			System.out.println(" 4-Numero por classe (normalizado)");
			graph = in.nextInt();

			switch (graph) {
				case 1:
					graphResults = new double[1][gen + 1];
					for (int i = 0; i < gen + 1; i++) {
						graphResults[0][i] = totalPopulationChange[i];
					}
					graphTitle = "Número Total De Individuos";
					resulType = "Número Total De Individuos";
					xLine = "Momento";
					yLine = "Dimensão da população";
					graphType = 1;
					break;
				case 2:
					graphResults = new double[1][gen];
					for (int i = 0; i < gen; i++) {
						graphResults[0][i] = rateOfChange[i];
					}
					graphTitle = "Crescimento da população";
					resulType = "Crescimento da população";
					xLine = "Momento";
					yLine = "Variação";
					graphType = 2;
					break;
				case 3:
					graphResults = new double[gen + 1][dim];
					for (int i = 0; i < gen + 1; i++) {
						for (int j = 0; j < dim; j++) {
							graphResults[i][j] = numberOfClasses[i][j];
						}
					}
					graphTitle = "Número por Classe (não normalizado)";
					resulType = "Número por Classe";
					xLine = "Momento";
					yLine = "Classe";
					graphType = 3;
					break;
				case 4:
					graphResults = new double[gen + 1][dim];
					for (int i = 0; i < gen + 1; i++) {
						double total = totalPopulationChange[i];
						for (int j = 0; j < dim; j++) {
							if (total == 0) {
								graphResults[i][j] = 0;
							} else {
								graphResults[i][j] = 100 * numberOfClasses[i][j] / total;
							}
						}
					}
					graphTitle = "Número por Classe (normalizado)";
					resulType = "Número por Classe";
					xLine = "Momento";
					yLine = "Classe";
					graphType = 4;
					break;
				default:
					System.out.println("Escolha inválida.");
					graph = -1;
					break;
			}
		}
		createGraph(graphResults, 0, graphTitle, resulType, xLine, yLine, "");

		save = -1;
		while (save == -1) {
			System.out.println("Guardar como: ");
			System.out.println(" 1-png");
			System.out.println(" 2-txt");
			System.out.println(" 3-eps");
			System.out.println(" 0-Não guardar");
			save = in.nextInt();

			if (save < 0 || save > 3) {
				System.out.println("Escolha inválida.");
				save = -1;
			}
		}
		if (save != 0) {
			System.out.println("Nome do ficheiro: ");
			if (in.hasNextLine()) {
				in.nextLine();
			}
			fileName = in.nextLine();

			String defaultExtension = "";
			switch (save) {
				case 1:
					defaultExtension = ".png";
					break;
				case 2:
					defaultExtension = ".txt";
					break;
				case 3:
					defaultExtension = ".eps";
					break;
			}
			if (!fileName.toLowerCase().endsWith(defaultExtension)) {
				fileName = fileName + defaultExtension;
			}
			String data = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
			createGraph(graphResults, save, graphTitle, resulType, xLine, yLine, populationName + data + fileName);
		}
		creatingTxtFileGraph(leslie, gen, totalPopulationChange, 
				rateOfChange, numberOfClasses, eigenvalue, eigenvector, true,
				true, true);
    }
    
    public static void nonInteractiveWay(String[] args) throws IOException {
    	int dim = -1;
        int gen = -1;
        int graph = -1;
        int save = -1;
        String fileName = "";
        String populationName = "";
        String outputFileGraphFormat = "";
        
    	int[] vec = new int[3];
		int pos_t = -1, pos_g = -1;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-t")) {
				pos_t = i;
			}
			if (args[i].equals("-g")) {
				pos_g = i;
			}
			if (args[i].equals("-e")) {
				vec[0] = 1;
			}
			if (args[i].equals("-v")) {
				vec[1] = 1;
			}
			if (args[i].equals("-r")) {
				vec[2] = 1;
			}
		}

		String generations_aux = args[pos_t + 1];
		int generations = Integer.parseInt(generations_aux); //gerações a calcular
		String format_gnuplot_files_aux = args[pos_g + 1];
		int format_gnuplot_files = Integer.parseInt(format_gnuplot_files_aux); //formato do ficheiro a executar

		do {
			System.out.println("Número de grupos etários (dimensão): ");
			dim = getdimfromLeslieMatrixFile(args[args.length-2]);
			System.out.println(dim); //Grupos etários
		} while (dim < 0);

		double [][] graphResults = new double [1][1];
		double[] totalPopulationChange = new double[generations+1];;
		double [] rateOfChange = new double [generations];
		double [][] numberOfClasses = new double[generations+1][dim];
		double[][] leslie = new double[dim][dim];
		double[][] population = new double[dim][1];

		File f = new File(args[args.length-2]);
		populationName = f.getName();
		if(populationName.toLowerCase().endsWith(".txt")) {
			populationName = populationName.substring(0, populationName.length()-4);
		}

		try {
			File file = new File(args[args.length - 1]);
			FileWriter writer = new FileWriter(args[args.length - 1]);

			System.out.println("Ficheiro criado: " + file.getName());
			writer.write("Gerações: \n" + generations + "\n");
			writer.write("Formato ficheiro gnuplot: \n" + format_gnuplot_files + "\n");

			//Retirar do ficheiro Matrix de Leslie e População
			leslie = MatrixWriteFile(args[args.length - 2],dim);
			Matrix leslie_f = convertToMatrix(leslie);
			writer.write("\nMatriz de Leslie: \n");
			writer.write(leslie_f + "\n");

			writer.write("Valores da população Inicial: "+ "\n");
			population = getPopulationfromFile(args[args.length-2],dim);
			Matrix populatin_f = convertToMatrix(population);
			writer.write(populatin_f + "\n");

			switch(format_gnuplot_files) {
				case 2:
					outputFileGraphFormat = ".txt";
					break;
				case 3:
					outputFileGraphFormat = ".eps";
					break;
				default:
					outputFileGraphFormat = ".png";
					break;
			}

			if (vec[0] == 1) {
				writer.write("Valor Próprio: \n" + eigen_value(leslie) + "\n");
				writer.write("Vetor Próprio: \n" + Arrays.toString(eigen_vec(leslie)) + "\n");

			}
			if (vec[1] == 1) {
				double value = -1;
				for(int i = 0; i <= generations; i++) {			
					Matrix populationResult = dimPopulationinT(leslie, population, i);
					totalPopulationChange[i] = totaldimPopulation(populationResult);
					value = totalPopulationChange[i];
					value = value * 100;
					value = Math.ceil(value);
					value = value / 100;
					writer.write("\nDimensão da população em t = " + i + "\n");
					writer.write(populationResult.toString()+ "\n");
					writer.write("Total da população em t = " + i+ "\n");
					writer.write(value + "\n");
				}

				graphResults = new double[1][generations+1];
				for (int i = 0; i < generations+1; i++) {
					graphResults[0][i] = totalPopulationChange[i];
				}
				String data1 = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
				createGraph(graphResults, format_gnuplot_files, "Número Total De Individuos", "Número Total De Individuos", "Momento", "Dimensão da população", populationName+data1+"NúmeroTotalDeIndividuos"+outputFileGraphFormat);
			}
			if (vec[2] == 1) {
				rateOfChange = rateofchange(leslie, population, generations);
				double value = -1;
				writer.write("\nTaxa de variação ao longo dos anos: "+ "\n");
				for(int i = 0; i < generations; i++) {
					value = rateOfChange[i];
					value = value * 100;
					value = Math.ceil(value);
					value = value / 100;
					writer.write(value + "\n");
				}

				graphResults = new double[1][generations];
				for(int i = 0; i < generations; i++) {
					graphResults[0][i] = rateOfChange[i];
				}
				String data2 = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
				createGraph(graphResults, format_gnuplot_files, "Crescimento da população", "Crescimento da população", "Momento", "Variação", populationName+data2+"CrescimentoDaPopulação"+outputFileGraphFormat);
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Ocorreu um erro ao escrever/criar o ficheiro.");
			e.printStackTrace();
		}
		
		for(int i = 0; i <= generations; i++) {
			Matrix populationResult = dimPopulationinT(leslie, population, i);
			totalPopulationChange[i] = totaldimPopulation(populationResult);
		}
		for(int i = 0; i <= generations; i++) {
			numberOfClasses [i] = dimPopulationinT(leslie, population, i).getColumn(0).toDenseVector().toArray();
		}
		String data3 = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

		graphResults = new double[generations+1][dim];
		for(int i = 0; i < generations+1; i++) {
			for(int j = 0; j < dim; j++) {
				graphResults[i][j] = numberOfClasses[i][j];
			}
		}
		createGraph(graphResults, format_gnuplot_files, "Número por Classe (não normalizado)", "Número por Classe", "Momento", "Classe", populationName+data3+"NúmeroporClasse(NãoNormalizado)"+outputFileGraphFormat);

		graphResults = new double[generations+1][dim];
		for(int i = 0; i < generations+1; i++) {
			double total = totalPopulationChange[i];
			for(int j = 0; j < dim; j++) {
				if(total == 0) {
					graphResults[i][j] = 0;
				} else {
					graphResults[i][j] = 100*numberOfClasses[i][j]/total;
				}
			}
		}
		createGraph(graphResults, format_gnuplot_files, "Número por Classe (normalizado)", "Número por Classe", "Momento", "Classe", populationName+data3+"NúmeroporClasse(Normalizado)"+outputFileGraphFormat);

		double valor = eigen_value(leslie);
		double [] vetor = eigen_vec(leslie);
		creatingTxtFileGraph(leslie, generations, totalPopulationChange, 
				rateOfChange, numberOfClasses, valor, 
				vetor, vec[0] == 1, vec[1] == 1, vec[2] == 1);
    }
    
    public static double [][] convertToDouble(Matrix matrix){
        int dim = matrix.columns();
        int gen = matrix.rows();
        double[][] doubles=new double[gen][dim];

        for(int i =0; i<gen;i++){
            for(int j=0;j<dim;j++){
                doubles[i][j] = matrix.get(i,j);
            }
        }

        return doubles;
    }

    public static double[][] LeslieMatrix (int dim) {
        Scanner in = new Scanner(System.in);
        double[][] leslie_matrix = new double[dim][dim];

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                leslie_matrix[i][j] = 0.0;
            }
        }

        System.out.println("Introduza a taxa de sobrevivência pela ordem de grupos etários: ");

        for (int i = 0; i < dim - 1; i++) {
            double survival = in.nextDouble();
            leslie_matrix[i + 1][i] = survival;
        }

        System.out.println("Introduza a taxa de fecundidade pela ordem de grupos etários: ");

        for (int j = 0; j < dim; j++) {
            double fecundity = in.nextDouble();
            leslie_matrix[0][j] = fecundity;
        }

        return leslie_matrix;
    }

    public static Matrix convertToMatrix(double[][] leslie) {
        Matrix leslie_matrix = new Basic2DMatrix(leslie);
        return leslie_matrix;
    }

    /***
     *
     * @param filename
     * @param dim
     * @return Matrix com a população inicial
     */
    public static double[][] getPopulationfromFile(String filename, int dim) throws FileNotFoundException {
        String[] vec = initial_vec(filename);

        String[] quantity_population = new String[dim];

        Scanner scanner = new Scanner(new File(filename));
        String line = "";

        for (int k = 0; k < vec.length; k++) {
            if (vec[k].equals("x")) {
                line = scanner.nextLine();
                quantity_population = line.split(",");

                for (int i = 0; i < quantity_population.length; i++) {
                    quantity_population[i] = quantity_population[i].trim();
                    quantity_population[i] = quantity_population[i].substring(4);
                }
            }
        }

        double[][] matrixpop = new double[quantity_population.length][1];

        for(int i =0; i<quantity_population.length; i++){
        	matrixpop[i][0] = Double.parseDouble(quantity_population[i]);
        }

        return matrixpop;
    }

    public static double[][] MatrixWriteFile(String filename, int dim) throws FileNotFoundException {
        String[] vec = initial_vec(filename);

        String[] survival = new String[dim];
        String[] fecundity = new String[dim];
        String[] quantity_population = new String[dim];

        Scanner scanner = new Scanner(new File(filename));
        String line = "";

        for (int k = 0; k < vec.length; k++) {
            if (vec[k].equals("x")) {
                line = scanner.nextLine();
                quantity_population = line.split(",");

                for (int i = 0; i < quantity_population.length; i++) {
                    quantity_population[i] = quantity_population[i].trim();
                    quantity_population[i] = quantity_population[i].substring(4);
                }
            } else if (vec[k].equals("s")) {
                line = scanner.nextLine();
                survival = line.split(",");

                for (int i = 0; i < survival.length; i++) {
                    survival[i] = survival[i].trim();
                    survival[i] = survival[i].substring(3);
                }
            } else if (vec[k].equals("f")) {
                line = scanner.nextLine();
                fecundity = line.split(",");

                for (int i = 0; i < fecundity.length; i++) {
                    fecundity[i] = fecundity[i].trim();
                    fecundity[i] = fecundity[i].substring(3);
                }
            }
        }

        double[][] matrixleslie = new double[fecundity.length][fecundity.length];

        //Sobrevivência:
        for (int i = 0; i < dim - 1; i++) {
            matrixleslie[i + 1][i] = Double.parseDouble(survival[i]);
        }
        //Fecundidade
        for (int j = 0; j < dim; j++) {
            matrixleslie[0][j] = Double.parseDouble(fecundity[j]);
        }

        return matrixleslie;
    }

    public static Matrix LeslieMatrixFile(String filename, int dim) throws FileNotFoundException {

        String[] vec = initial_vec(filename);

        String[] survival = new String[0];
        String[] fecundity = new String[0];
        String[] quantity_population = new String[0];

        Scanner scanner = new Scanner(new File(filename));
        String line = "";

        for (int k = 0; k < vec.length; k++) {
            if (vec[k].equals("x")) {
                line = scanner.nextLine();
                System.out.println("\nQuantidade de população:");
                quantity_population = line.split(",");

                for (int i = 0; i < quantity_population.length; i++) {
                    quantity_population[i] = quantity_population[i].trim();
                    quantity_population[i] = quantity_population[i].substring(4);

                    System.out.println(quantity_population[i]);
                }
            } else if (vec[k].equals("s")) {
                line = scanner.nextLine();
                System.out.println("\nSobrevivência:");
                survival = line.split(",");

                for (int i = 0; i < survival.length; i++) {
                    survival[i] = survival[i].trim();
                    survival[i] = survival[i].substring(3);
                    System.out.println(survival[i]);
                }
            } else if (vec[k].equals("f")) {
                line = scanner.nextLine();
                System.out.println("\nFecundidade:"); //Dimensão
                fecundity = line.split(",");

                for (int i = 0; i < fecundity.length; i++) {
                    fecundity[i] = fecundity[i].trim();
                    fecundity[i] = fecundity[i].substring(3);
                    System.out.println(fecundity[i]);
                }
            }
        }

        double[][] matrixleslie = new double[fecundity.length][fecundity.length];

        //Sobrevivência:
        for (int i = 0; i < dim - 1; i++) {
            matrixleslie[i + 1][i] = Double.parseDouble(survival[i]);
        }
        //Fecundidade
        for (int j = 0; j < dim; j++) {
            matrixleslie[0][j] = Double.parseDouble(fecundity[j]);
        }

        Matrix matrix = convertToMatrix(matrixleslie);

        System.out.println("\nMatriz de Leslie:");
        return matrix;
    }

    public static int getdimfromLeslieMatrixFile(String filename) throws FileNotFoundException {
        String[] vec = initial_vec(filename);
        String[] quantity_population = new String[0];

        Scanner scanner = new Scanner(new File(filename));
        String line = "";

        for (int k = 0; k < vec.length; k++) {
            if (vec[k].equals("x")) {
                line = scanner.nextLine();
                quantity_population = line.split(",");

                for (int i = 0; i < quantity_population.length; i++) {
                    quantity_population[i] = quantity_population[i].trim();
                    quantity_population[i] = quantity_population[i].substring(4);

                }
            }
        }

        return quantity_population.length;
    }

    public static String[] initial_vec(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        String[] vec = new String[3];

        String line = scanner.nextLine();
        String[] firstLine = line.split("0");

        line = scanner.nextLine();
        String[] secondLine = line.split("0");

        line = scanner.nextLine();
        String[] thirdLine = line.split("0");

        vec[0] = firstLine[0];
        vec[1] = secondLine[0];
        vec[2] = thirdLine[0];

        return vec;
    }

    public static double eigen_value(double[][] matrix_leslie) {
        // Criar objeto do tipo Matriz
        Matrix a = new Basic2DMatrix(matrix_leslie);

        //Obtem valores e vetores próprios fazendo "Eigen Decomposition"
        EigenDecompositor eigenD = new EigenDecompositor(a);
        Matrix[] mattD = eigenD.decompose();

        //Converte objeto Matrix (duas matrizes) para array Java
        double[][] matA = mattD[0].toDenseMatrix().toArray();
        double[][] matB = mattD[1].toDenseMatrix().toArray(); //Dá-nos o valor próprio

        double max_eigen_value = -1;

        for (int i = 0; i < matB.length; i++) {
            for (int j = 0; j < matB.length; j++) {
                if (max_eigen_value < matB[i][j]) {
                    max_eigen_value = matB[i][j];
                }
            }
        }

        max_eigen_value = max_eigen_value * 10000;
        max_eigen_value = Math.ceil(max_eigen_value);
        max_eigen_value = max_eigen_value / 10000;

        return max_eigen_value;
    }

    public static double[] eigen_vec(double[][] matrix_leslie) {
        int count = 0;

        // Criar objeto do tipo Matriz
        Matrix a = new Basic2DMatrix(matrix_leslie);

        //Obtem valores e vetores próprios fazendo "Eigen Decomposition"
        EigenDecompositor eigenD = new EigenDecompositor(a);
        Matrix[] mattD = eigenD.decompose();

        //Converte objeto Matrix (duas matrizes) para array Java
        double[][] matA = mattD[0].toDenseMatrix().toArray();
        double[][] matB = mattD[1].toDenseMatrix().toArray();

        double max_eigen_value = -1;

        //Faz a contagem para saber em que posição está o valor próprio
        for (int i = 0; i < matB.length; i++) {
            for (int j = 0; j < matB.length; j++) {
                if (max_eigen_value < matB[i][j]) {
                    count++;
                    max_eigen_value = matB[i][j];
                }
            }
        }

        //Matriz A:
        double[] eigen_vec = new double[matA.length];
		double value = -1;

        if (count > 0) {
            for (int i = 0; i < matA.length; i++) {
            	value = matA[i][count - 1];
				value = value * 100;
				value = Math.ceil(value);
				value = value / 100;
                eigen_vec[i] = value;
            }
        } else {
            System.out.println("Line for eigen vector not found.");
        }

        return eigen_vec;
    }

    /***
     * Calcular dimensão de População em Determinado momento
     * Parametros : População inicial, taxa de sobrevivencia, taxa de fecundidade e valor de tempo ou
     * Matrix Leslie + Matriz população incial e valor de tempo
     *
     * Output: Valor da dimensão da população em t
     */
    public static Matrix dimPopulationinT(double[][] leslie, double[][] population, int t ) {

        //Criação da Matrix em T
        double [][] populationinT = new double[1][population.length];
        Matrix populationinTMatrix = convertToMatrix(populationinT);
    	
        //Conversao em Matrizes para facilitar calculos
        Matrix lesliematrix = convertToMatrix(leslie);
        Matrix populationInicial = convertToMatrix(population);

        populationinTMatrix  = (lesliematrix.power(t)).multiply(populationInicial);
        return populationinTMatrix;
    }

    /***
     * Dimensão da população Reprodutora - Posso ter que multiplicar o valor por 2;
     * Recebe matrix e calcula a sua soma
     */
    public static double totaldimPopulation(Matrix population){
        return population.sum();
    }


    /***
     * Calculo da distribuição da população
     *
     */
    public static List<String> distriPopulation(double[][] leslie, double[][] population, int t){

        //Criação da Matrix em T
        double [][] populationinT = new double[1][population.length];
        Matrix populationDistribution = convertToMatrix(populationinT);

        //Conversao em Matrizes para facilitar calculos
        Matrix lesliematrix = convertToMatrix(leslie);
        Matrix populationInicial = convertToMatrix(population);

        List <String> distribution= new ArrayList<>();

        for (int i = 1; i <= t ; i++){

            populationDistribution  = (lesliematrix.power(i)).multiply(populationInicial);

            distribution.add(populationDistribution.toString());

            populationInicial = populationDistribution;
        }

        return distribution;

    }


    /***
     * Variação da população nos entre o inicio e os ano final dado
     * Parametros:População inicial, Matrix leslie e t final
     *
     * Taxa de variação segue a formula população ano t+1/população no ano t
     *
     * Output : taxa de variação ao longo dos anos - Lista de valores entre anos
     */
    public static double [] rateofchange(double[][] leslie,double[][] population, int t ){
    	double result [] = new double [t];
    	
    	for(int i = 0; i < t; i++) {
	    	double value1 = 0;
	    	double value2 = 0;
	    	
	    	value1 = totaldimPopulation(dimPopulationinT(leslie,population,i));
	    	value2 = totaldimPopulation(dimPopulationinT(leslie,population,i+1));
	    	
	    	if(value1 == 0) {
	    		result [i] = 0;
	    	} else {
	    		result [i] = value2/value1;
	    	}
    	}
    			
        return result;
    }
    
    public static void createGraph(double[][] matrix, int outputType, String graphTitle,
    		String resultType, String xLine, String yLine, String outputFileName) throws IOException {
    	int multipleLines = 0;
    	
    	if(matrix.length > 1) {
    		multipleLines = matrix[0].length;
    	}
    	creatingDataFile(matrix);
    	gnuplotGraph(outputType, graphTitle, resultType, xLine, yLine, outputFileName, multipleLines);
    	startingGnuplot();
    }
    
    /***
     * Desenho da representação gráfica da dimensão da populaçãao (total de indíviduos), 
     * a taxa de variação e a evolução da distribuição da população,
     * por classe, ao longo do tempo.
     * 
     * Esta função desenha os gráficos respetivos em formato png, txt e eps.
     * @throws IOException 
     */
    public static void gnuplotGraph(int outputType, String graphTitle, String dataDescription, 
    		String xLine, String yLine, String outputFileName, int multipleLines) throws IOException {
    	FileWriter plot = new FileWriter(plotNameFile);
    	String terminalOutput = "";
    	
    	switch(outputType) {
    	case 1:
    		terminalOutput = "png";
    		break;
    	case 2:
    		terminalOutput = "dumb";
    		break;
    	case 3:
    		terminalOutput = "postscript";
    		break;
    	default:
    		terminalOutput = "qt";
    		break;
    	}
    	
    	plot.write(String.format("set terminal %s\n", terminalOutput));
    	if(outputType >= 1 && outputType <= 3) {
    		plot.write(String.format("system \"mkdir %s\"\n", outputDir));
    		plot.write(String.format("set output \"%s/%s\"\n", outputDir, outputFileName));
    	}
    	plot.write(String.format("set title \"%s\"\n", graphTitle));
    	plot.write(String.format("set xlabel \"%s\"\n", xLine));
    	plot.write(String.format("set ylabel \"%s\"\n", yLine));
    	plot.write(String.format("set style data linespoints\n"));
    	if(multipleLines > 0) {
    		plot.write(String.format("plot"));
    		for(int i = 0; i < multipleLines; i++) {
    			plot.write(String.format(" \"%s\" using 1:%d with lines title \"x%d\", ", dataNameFile, i+2, i+1));
    		}
    	} else {
    		plot.write(String.format("plot \"%s\" title \"%s\"\n", dataNameFile, dataDescription));
    	}
    	plot.close();
    }
    
    public static void startingGnuplot() throws IOException {
    	String result = String.format("\"%s\" -p \"%s\"", pathGnuplot, plotNameFile);
    	Runtime  rt = Runtime.getRuntime(); 
    	Process prcs = rt.exec(result);
    	try {
			prcs.waitFor();
		} catch (InterruptedException e) {
		}
    }
    
    public static void creatingDataFile(double[][] matrix) throws IOException {
    	FileWriter data = new FileWriter(dataNameFile);
    	
    	if(matrix.length == 1) {
    		for(int i = 0; i < matrix[0].length; i++) {
        		data.write(String.format(Locale.US, "%d %.2f\n", i, matrix[0][i]));
        	}
    	} else {
	    	for(int i = 0; i < matrix.length; i++) {
		    	data.write(String.format("%d", i));
		    	for(int j = 0; j < matrix[i].length; j++) {
		    		data.write(String.format(Locale.US, ", %.2f", matrix[i][j]));
		    	}
		    	data.write("\n");
	    	}
    	}
    	data.close();
    }
    
    public static void creatingTxtFileGraph(double[][] lesliMatrix, int gen, double[] dimPopulation, 
    		double[] rateOfChange, double[][] classes, double eigenvalue, double[] eigenvector, 
    		boolean useValueAndVector, boolean useDimPopulation, boolean useRateOfChange) throws IOException {
    	FileWriter result = new FileWriter("TextGraphResult.txt");
    	
    	result.write(String.format("K=%d\n", gen));
    	result.write(String.format("Matriz de Leslie\n"));
    	for(int i = 0; i < lesliMatrix.length; i++) {
    		for(int j = 0; j < lesliMatrix.length; j++) {
    			if(j > 0) {
    				result.write(", ");
    			}
    			result.write(String.format(Locale.US, "%.2f", lesliMatrix[i][j]));
    		}
    		result.write(String.format("\n"));
    	}
    	if(useDimPopulation) {
	    	result.write(String.format("\nNumero total de individuos\n"));
	    	result.write(String.format("(t, Nt)\n"));
	    	for(int i = 0; i < gen+1; i++) {
	    		result.write(String.format(Locale.US, "(%d, %.2f)\n", i, dimPopulation[i]));
	    	}
    	}
    	if(useRateOfChange) {
	    	result.write(String.format("\n\nCrescimento da população\n"));
	    	result.write(String.format("(t, delta_t)\n"));
	    	for(int i = 0; i < gen; i++) {
	    		result.write(String.format(Locale.US, "(%d, %.2f)\n", i, rateOfChange[i]));
	    	}
    	}
    	result.write(String.format("\n\nNumero por classe (não normalizado)\n"));
    	result.write(String.format("%s\n", getHeader(lesliMatrix.length)));
    	for(int i = 0; i < gen+1; i++) {
    		result.write(String.format("(%d", i));
    		for(int j = 0; j < lesliMatrix.length; j++) {
    			result.write(String.format(Locale.US, ", %.2f", classes[i][j]));
    		}
    		result.write(String.format(")\n"));
    	}
    	result.write(String.format("\n\nNumero por classe (normalizado)\n"));
    	result.write(String.format("%s\n", getHeader(lesliMatrix.length)));
    	for(int i = 0; i < gen+1; i++) {
    		result.write(String.format("(%d", i));
    		for(int j = 0; j < lesliMatrix.length; j++) {
    			result.write(String.format(Locale.US, ", 100*%.2f/%.2f", classes[i][j], dimPopulation[i]));
    		}
    		result.write(String.format(")\n"));
    	}
    	if(useValueAndVector) {
	    	result.write(String.format("\n\nMaior valor próprio e vetor associado\n"));
	    	result.write(String.format(Locale.US, "lambda=%.4f\n", eigenvalue));
	    	result.write(String.format("vetor proprio associado=("));
	    	for(int i = 0; i < eigenvector.length; i++) {
	    		if(i > 0) {
	    			result.write(", ");
	    		}
	    		result.write(String.format(Locale.US, "%.2f", eigenvector[i]));
	    	}
	    	result.write(")\n");
    	}
    	result.close();
    }
    
    public static String getHeader(int length) {
    	String header = "(t";
    	for(int i = 0; i < length; i ++) {
    		header += String.format(", x%d", i+1);
    	}
    	header += ")";
    	
    	return header;
    }

    public static boolean order_class(String filename) throws FileNotFoundException {
    	Scanner scanner = new Scanner(new File(filename));
    	String[] vec = initial_vec(filename);

		String line = scanner.nextLine();
		String[] firstPart = line.split(",");
		String aux = "";
		int order = -1;
		int aux_value = -1;
		boolean flag = false;

		for (int i = 0; i < vec.length; i++) {
			if (vec[i].equals("x")) {
				order = -1;

				for (int j = 0; j < firstPart.length; j++) {
					firstPart[j] = firstPart[j].trim();
					aux = firstPart[j].substring(2, 3);
					aux_value = Integer.parseInt(aux);

					if (aux_value == order + 1) {
						order++;
					} else {
						flag = true;
					}
				}
			} else if (vec[i].equals("s")) {
				order = -1;

				line = scanner.nextLine();
				firstPart = line.split(",");

				for (int j = 0; j < firstPart.length; j++) {
					firstPart[j] = firstPart[j].trim();
					aux = firstPart[j].substring(1, 2);
					aux_value = Integer.parseInt(aux);

					if (aux_value == order + 1) {
						order++;
					} else {
						flag = true;
						break;
					}
				}
			}
			else if (vec[i].equals("f")) {
				order = -1;
				line = scanner.nextLine();
				firstPart = line.split(",");

				for (int j = 0; j < firstPart.length; j++) {
					firstPart[j] = firstPart[j].trim();
					aux = firstPart[j].substring(1, 2);
					aux_value = Integer.parseInt(aux);

					if (aux_value == order + 1) {
						order++;
					} else {
						flag = true;
						break;
					}
				}
			}
		}
		if (flag) {
			System.out.println("A ordem dos valores está incorreta no ficheiro de entrada.");
			return false;
		}
		else {
			return true;
		}
	}
}
