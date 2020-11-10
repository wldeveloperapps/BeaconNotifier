package com.example.beaconnotifier;

/*
    KalmanFilter test = new KalmanFilter(0.008, 0.1);
            double[] testData = {66,64,63,63,63,66,65,67,58};
            for(double x: testData){
            System.out.println("Input data: " + x);
            System.out.println("Filtered data: " + test.filter(x));
            }'
            Example Usage with controlled input
            KalmanFilter test = new KalmanFilter(0.008, 0.1, 1, 1, 1);
            double[] testData = {66,64,63,63,63,66,65,67,58};
            double u = 0.2;
            for(double x: testData){
            System.out.println("Input data: " + x);
            System.out.println("Filtered data: " + test.filter(x, u));
            }

*/
public class KalmanFilter {

    private double A = 1;
    private double B = 0;
    private double C = 1;

    private double R;
    private double Q;

    private double cov = Double.NaN;
    private double x = Double.NaN;

    /**
     * Constructor
     *
     * @param R Process noise (0.008)
     * @param Q Measurement noise (deviation standard,3 or 4 con movimiento 20)
     * @param A State vector .How a previous state effects a new state
     * @param B Control vector. Here B is the control parameter and is multiplied by the control/action on each filter step
     * @param C Measurement vector
     *          R models the process noise and describes how noisy our system internally is
     *          Or, in other words, how much noise can we expect from the system itself
     *          As our system is constant we can set this to a (very) low value.
     *          Q resembles the measurement noise; how much noise is caused by our measurements
     *          As we expect that our measurements will contain most of the noise,
     *          it makes sense to set this parameter to a high number (especially in comparison to the process noise).
     *          The lower Q, the faster the system responds to changes (as it trusts the measurement more) but the more noisy it is.
     */
    public KalmanFilter(double R, double Q, double A, double B, double C) {
        this.R = R;
        this.Q = Q;

        this.A = A;
        this.B = B;
        this.C = C;

        this.cov = Double.NaN;
        this.x = Double.NaN; // estimated signal without noise
    }

    /**
     * Constructor
     *
     * @param R Process noise
     * @param Q Measurement noise
     */
    public KalmanFilter(double R, double Q) {
        this.R = R;
        this.Q = Q;

    }


    /**
     * Filters a measurement
     *
     * @param measurement The measurement value to be filtered
     * @param u           The controlled input value
     * @return The filtered value
     */
    public double filter(double measurement, double u) {
        if (Double.isNaN(this.x)) {
            this.x = (1 / this.C) * measurement;
            this.cov = (1 / this.C) * this.Q * (1 / this.C);
        } else {
            double predX = (this.A * this.x) + (this.B * u);
            double predCov = ((this.A * this.cov) * this.A) + this.R;

            // Kalman gain
            double K = predCov * this.C * (1 / ((this.C * predCov * this.C) + this.Q));

            // Correction
            this.x = predX + K * (measurement - (this.C * predX));
            this.cov = predCov - (K * this.C * predCov);
        }
        return this.x;
    }

    /**
     * Filters a measurement
     *
     * @param measurement The measurement value to be filtered
     * @return The filtered value
     */
    public double filter(double measurement) {
        return filter(measurement, 0);
    }


    public double getLastCov() {
        return this.cov;
    }

    public double getLastX() {
        return this.x;
    }

    /**
     * Filters a measurement
     */
    public double filter(double measurement, double lastX, double lastCov) {
        this.x = lastX;
        this.cov = lastCov;
        return filter(measurement, 0);
    }

    /**
     * Get the last measurement.
     *
     * @return The last measurement fed into the filter
     */
    public double lastMeasurement() {
        return this.x;
    }

    /**
     * Sets measurement noise
     *
     * @param noise The new measurement noise
     */
    public void setMeasurementNoise(double noise) {
        this.Q = noise;
    }

    /**
     * Sets process noise
     *
     * @param noise The new process noise
     */
    public void setProcessNoise(double noise) {
        this.R = noise;

    }
}