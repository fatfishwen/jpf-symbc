package mnist2;

import gov.nasa.jpf.symbc.Debug;

public class DNNt {

	private InternalData internal;

	// weights0: shape is 3x3x1x2
	// biases0: shape is 2
	// weights2: shape is 3x3x2x4
	// biases2: shape is 4
	// weights6: shape is 576
	// biases6: shape is 128
	// weights8: shape is 128
	// biases8: shape is 10

	public DNNt(InternalData internal) {
		this.internal = internal;
	}

	// the DNN input is of shap 28x28x1
	int run(double[][][] input) {

		// layer 0: conv2d_1
		double[][][] layer0 = new double[26][26][2];
		for (int i = 0; i < 26; i++)
			for (int j = 0; j < 26; j++)
				for (int k = 0; k < 2; k++) {
					layer0[i][j][k] = internal.biases0[k];
					for (int I = 0; I < 3; I++)
						for (int J = 0; J < 3; J++)
							for (int K = 0; K < 1; K++)
								layer0[i][j][k] += internal.weights0[I][J][K][k] * input[i + I][j + J][K];
				}

		// layer 1: activation_1
		double[][][] layer1 = new double[26][26][2];
		for (int i = 0; i < 26; i++)
			for (int j = 0; j < 26; j++)
				for (int k = 0; k < 2; k++)
					if (layer0[i][j][k] > 0)
						layer1[i][j][k] = layer0[i][j][k];
					else
						layer1[i][j][k] = 0;

        // layer 2: conv2d_2
        double[][][] layer2 = new double[24][24][4];
        for (int i = 0; i < 24; i++)
            for (int j = 0; j < 24; j++)
                for (int k = 0; k < 4; k++) {
                    layer2[i][j][k] = internal.biases2[k];
                    for (int I = 0; I < 3; I++)
                        for (int J = 0; J < 3; J++)
                            for (int K = 0; K < 2; K++)
                                layer2[i][j][k] += internal.weights2[I][J][K][k] * layer1[i + I][j + J][K];
                }

        // layer 3: activation_2
        double[][][] layer3 = new double[24][24][4];
        for (int i = 0; i < 24; i++)
            for (int j = 0; j < 24; j++)
                for (int k = 0; k < 4; k++)
                    if (layer2[i][j][k] > 0)
                        layer3[i][j][k] = layer2[i][j][k];
                    else
                        layer3[i][j][k] = 0;

        // layer 4: max_pooling2d_1
        double[][][] layer4 = new double[12][12][4];
        for (int i = 0; i < 12; i++)
            for (int j = 0; j < 12; j++)
                for (int k = 0; k < 4; k++) {
                    layer4[i][j][k] = 0;
                    for (int I = i * 2; I < (i + 1) * 2; I++)
                        for (int J = j * 2; J < (j + 1) * 2; J++)
                            if (layer3[I][J][k] > layer4[i][j][k])
                                layer4[i][j][k] = layer3[I][J][k];
                }

        // layer 5: flatten_1
        double[] layer5 = new double[576];
        for (int i = 0; i < 576; i++) {
            int d0 = i / 48;
            int d1 = (i % 48) / 4;
            int d2 = i - d0 * 48 - d1 * 4;
            layer5[i] = layer4[d0][d1][d2];
        }

        // layer 6: dense_1
        double[] layer6 = new double[128];
        for (int i = 0; i < 128; i++) {
            layer6[i] = internal.biases6[i];
            for (int I = 0; I < 576; I++)
                layer6[i] += internal.weights6[I][i] * layer5[I];
        }

        // layer 7: activation_3
        double[] layer7 = new double[128];
        for (int i = 0; i < 128; i++)
            if (layer6[i] > 0)
                layer7[i] = layer6[i];
            else
                layer7[i] = 0;

        // layer 8: dense_2
        double[] layer8 = new double[10];
        for (int i = 0; i < 10; i++) {
            layer8[i] = internal.biases8[i];
            for (int I = 0; I < 128; I++)
                layer8[i] += internal.weights8[I][i] * layer7[I];
        }

        // layer 9: activation_4
        int ret = 0;
        double res = -100000;
        for (int i = 0; i < 10; i++) {
            if (layer8[i] > res) {
                res = layer8[i];
                ret = i;
            }
        }
        return ret;
	}

	public static void main(String[] args) {

		final int IMG_HEIGHT = 28;
		final int IMG_WIDTH = 28;

		/* Initial concrete Input. */
		final byte[][] inputImage = {
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } };

		/* Construct symcrete input. */
		System.out.println("Construct symcrete input..");
		double[][][] a = new double[IMG_HEIGHT][IMG_WIDTH][1];
		for (int i = 0; i < IMG_HEIGHT; i++) {
			for (int j = 0; j < IMG_WIDTH; j++) {
				for (int k = 0; k < 1; k++) {

					/* Normalize value from [-128,127] to be in range [0, 1] */
					double value = (inputImage[i][j] + 128) / 255.0;

					/* Add symbolic value. */
					//if ((i==0 && j==0)) {
					//if ((i==0 && j==0) || (i==2 && j==2)) {
					if (i<10) {
						a[i][j][k] = Debug.addSymbolicDouble(value, "sym_" + i + "_" + j + "_" + k + "_" + String.valueOf(value));
					} else  {
						a[i][j][k] = value;
					}

				}
			}
		}

		/* Retrieve internal data for DNN. */
		System.out.println("Load internal data..");
		InternalData internalData = new InternalData();
		internalData.biases0 = Debug.getBiases0();
		internalData.biases2 = Debug.getBiases2();
		internalData.biases6 = Debug.getBiases6();
		internalData.biases8 = Debug.getBiases8();
		internalData.weights0 = Debug.getWeights0();
		internalData.weights2 = Debug.getWeights2();
		internalData.weights6 = Debug.getWeights6();
		internalData.weights8 = Debug.getWeights8();
		
		/* Run DNN. */
		System.out.println("Run DNN..");
		DNNt dnn = new DNNt(internalData);
		int res = dnn.run(a);
		
		System.out.println();
		System.out.println("Result: " + res);
		//Debug.printPC("pc=");
		
		System.out.println("Done.");
	}

}
