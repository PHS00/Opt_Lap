package Cap;

import java.io.BufferedWriter;
import java.io.IOException;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

public class capstone {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		model1();
	}

	public static void model1() throws IOException {
		GetData.main(null);
		BufferedWriter bw;
		int n = GetData.n;
		int K = GetData.K;
		int Q = GetData.Q;

		double[][] c = new double[n][n]; // 비용
		int[] p = new int[n]; // pickup 개수
		int[] d = new int[n]; // delivery 개수
		// int[] s = new int[n]; //추가 서비스시간
		double[][][] std = new double[n][n][K]; // 표준편차
		double[][][] v = new double[n][n][K]; // 속력
		double[][] D = new double[n][n]; // 거리
		double s[] = new double[n];	// i node의 서비스시간 

		D = GetData.d;
		d = GetData.delivery;
		p = GetData.pickup;

		for (int k = 0; k < K; k++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					v[i][j][k] = GetData.speed[i][j];
					std[i][j][k] = GetData.std[i][j];
				}
			}
		}

//		d[7] = 0;
		for (int i = 0; i < n; i++) {
			p[i] += 10;
		}
		double[][] cost = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				cost[i][j] = (D[i][j] * 10) / (GetData.speed[i][j] / 60. * 1000);
			}
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j)
					c[i][j] = 0;
				else
					c[i][j] = cost[i][j];
			}
		}

//		for(int i=0; i<n;i++) {
//			for(int j=0; j<n; j++) {
//				c[i][j] = D[i][j];
//			}
//		}
		double PI = 1.2816;
		double A = 0;
		// double a =0.1;
		double a, b, e;
		a = 1.5;
		b = 1 / 3;
		e = 0.5;

		double[] B = new double[K];
		for (int k = 0; k < K; k++) {
			B[k] = 307;
		}
		// 30 -> 해가 바뀌면서 솔루션 나옴

		for (int i = 0; i < n; i++) {
			s[i] = (a + e) * d[i] + b * p[i];
		}
		try {
			IloCplex cplex = new IloCplex();

			// 결정변수
			IloNumVar[][][] x = new IloNumVar[n][n][K];
			IloNumVar[][] y = new IloNumVar[n][];
			IloNumVar[][] z = new IloNumVar[n][];
			IloNumVar[] w = new IloNumVar[K];

			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					x[i][j] = cplex.numVarArray(K, 0, 1, IloNumVarType.Int);
				}
			}
			for (int i = 0; i < n; i++) {
				y[i] = cplex.numVarArray(n, 0, Double.MAX_VALUE);
				z[i] = cplex.numVarArray(n, 0, Double.MAX_VALUE);
			}
			for (int i = 0; i < K; i++) {
				w[i] = cplex.numVar(0, Double.MAX_VALUE);
			}
			int sum = 0;
			// 목적함수
			IloLinearNumExpr objective = cplex.linearNumExpr();
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < K; k++) {
						objective.addTerm(s[j], x[i][j][k]);
						objective.addTerm((c[i][j]), x[i][j][k]);

					}
				}
				// sum+=s[i];
			}

			cplex.addMinimize(objective);

			// 제약식

			// con1
			IloLinearNumExpr[] con1 = new IloLinearNumExpr[n];
			for (int i = 1; i < n; i++) {
				con1[i] = cplex.linearNumExpr();
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < K; k++) {
						con1[i].addTerm(1.0, x[i][j][k]);
					}
				}
				cplex.addEq(con1[i], 1);

			}

			// con2
			IloLinearNumExpr[][] con2 = new IloLinearNumExpr[n][K];
			for (int i = 0; i < n; i++) {
				for (int k = 0; k < K; k++) {
					con2[i][k] = cplex.linearNumExpr();
					for (int j = 0; j < n; j++) {
						con2[i][k].addTerm(1.0, x[i][j][k]);
						con2[i][k].addTerm(-1, x[j][i][k]);
					}
					cplex.addEq(con2[i][k], 0);
				}
			}

			// con3
			IloLinearNumExpr[] con3 = new IloLinearNumExpr[K];
			for (int k = 0; k < K; k++) {
				con3[k] = cplex.linearNumExpr();
				for (int i = 0; i < n; i++) {
					con3[k].addTerm(1.0, x[0][i][k]);
				}
				cplex.addLe(con3[k], 1);
			}

			// con4
			IloLinearNumExpr[][] con4 = new IloLinearNumExpr[n][n];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (i == j)
						continue;
					con4[i][j] = cplex.linearNumExpr();
					for (int k = 0; k < K; k++) {
						con4[i][j].addTerm(Q, x[i][j][k]);
					}
					cplex.addLe(cplex.sum(y[i][j], z[i][j]), con4[i][j]);
				}
			}

			// con5
			IloLinearNumExpr[] con5 = new IloLinearNumExpr[n];
			for (int i = 1; i < n; i++) {
				con5[i] = cplex.linearNumExpr();
				for (int j = 0; j < n; j++) {
					con5[i].addTerm(y[i][j], 1);
					con5[i].addTerm(y[j][i], -1);
				}
				cplex.addEq(con5[i], p[i]);
			}

			// con6
			IloLinearNumExpr[] con6 = new IloLinearNumExpr[n];
			for (int i = 1; i < n; i++) {
				con6[i] = cplex.linearNumExpr();
				for (int j = 0; j < n; j++) {
					con6[i].addTerm(z[j][i], 1);
					con6[i].addTerm(z[i][j], -1);
				}
				cplex.addEq(con6[i], d[i]);
			}

			// con7
			IloNumExpr[] con7 = new IloNumExpr[K];
			for (int k = 0; k < K; k++) {
				con7[k] = cplex.quadNumExpr();
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						double tmp = std[i][j][k] * std[i][j][k];
						con7[k] = cplex.sum(con7[k], cplex.prod(tmp, x[i][j][k], x[i][j][k]));
					}
				}
				con7[k] = cplex.sum(con7[k], cplex.prod(-1, w[k], w[k]));
				cplex.addLe(con7[k], 0);
			}

			// con8, con9
			IloLinearNumExpr[] con8 = new IloLinearNumExpr[K];
			IloLinearNumExpr[] con9 = new IloLinearNumExpr[K];
			for (int k = 0; k < K; k++) {
				con8[k] = cplex.linearNumExpr();
				con9[k] = cplex.linearNumExpr();
				for (int i = 0; i < n; i++) {
					for (int j = 0; j < n; j++) {
						con8[k].addTerm(D[i][j] / v[i][j][k], x[i][j][k]);
						con9[k].addTerm(s[i], x[j][i][k]);
					}
				}
				cplex.addLe(cplex.sum(con8[k], cplex.prod(PI, w[k])), cplex.sum(B[k], cplex.prod(-1, con9[k])));
			}

			// con10
			for (int i = 0; i < n; i++) {
				for (int k = 0; k < K; k++) {
					cplex.addEq(x[i][i][k], 0);
				}
			}

			long start = System.currentTimeMillis();
			cplex.solve();
			cplex.setParam(IloCplex.DoubleParam.TimeLimit, 3600);
			long end = System.currentTimeMillis();
			double time = (end - start) / 1000.0;

			String resultPath = "";
			String fileName = "";
			String txt = ".txt";
			String setting = "";

			resultPath = "C:\\Users\\최적화_연구실_PC1\\OneDrive - Chonnam National University\\바탕 화면\\Optimize\\Capstone\\Capstone\\";
			fileName = "P-n19-k2_Result";
			setting = "_PI" + PI + "-Bk" + B[0] + "-k" + K;
			bw = Write.getBw(resultPath + fileName + setting + txt);

			// print result
			System.out.println("obj = " + cplex.getObjValue());
			bw.write("obj = " + cplex.getObjValue());
			bw.newLine();

			System.out.println("PI : " + PI + " Bk : " + B[0] + " k : " + K);
			bw.write("PI : " + PI + " Bk : " + B[0] + " k : " + K);
			bw.newLine();

			System.out.println("총 얼마? = " + cplex.getObjValue() * 9500 / 60);
			for (int k = 0; k < K; k++) {
				int i = 0;
				int j = 0;
				double TotalTime = 0;
				while (j != n) {
					if (cplex.getValue(x[i][j][k]) > 0.5) {
						TotalTime += (D[i][j] * 10) / (GetData.speed[i][j] / 60. * 1000) + s[j];
						System.out.println((k + 1) + "차량 이동경로 : " + (i + 1) + " " + (j + 1) + "\t TotalTime: "
								+ (int) TotalTime + "분");
						bw.write((k + 1) + "차량 이동경로 : " + (i + 1) + " " + (j + 1) + "\t TotalTime: "
								+ (int) TotalTime + "분");
						bw.newLine();

						i = j;
						j = -1;
						if (i == 0)
							break;
					}
					j++;
				}
			}
			cplex.end();

			System.out.println("실행시간 = " + time + "초");
			bw.write("실행시간 = " + time + "초");
			bw.newLine();

			bw.close();

		} catch (IloException exc) {
			exc.printStackTrace();
		}
	}

}