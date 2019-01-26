package ru.shemplo.dm.course;

import static ru.shemplo.dm.course.Constants.*;
import static ru.shemplo.dm.course.Functions.*;
import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import ru.shemplo.dm.course.gfx.ProgressListener;

public class Processor {
    
    private final List <Double []> 
        Ts = new ArrayList <> (),
        As = new ArrayList <> (),
        Ws = new ArrayList <> ();
    
    @SuppressWarnings ("unused")
    private final double dt, dx, dx2, ti;
    private final int NODES, ITERATIONS;
    
    // TODO: call this method in separate thread from GUI thread
    public Processor (int nodes, int iteraiton, double dt, double dx, double ti) {
        this.NODES = nodes; this.ITERATIONS = iteraiton;
        this.dt = dt; this.dx = dx; this.ti = ti;
        this.dx2 = dx * dx;
        
        Double [] T0s = new Double [nodes];
        Arrays.fill (T0s, T0);
        Ts.add (T0s);
        
        Double [] A0s = new Double [nodes];
        Arrays.fill (A0s, 1.0D);
        As.add (A0s);
        
        Double [] W0s = new Double [nodes];
        for (int i = 0; i < nodes; i++) {
            W0s [i] = abs (W.apply (A0s [i], T0s [i]));
        }
        Ws.add (W0s);
    }
    
    private final void safeCallOfListener (ProgressListener listener, 
            Consumer <ProgressListener> consumer) {
        if (Objects.isNull (listener)) { return; }
        consumer.accept (listener);
    }
    
    public void computeWithListener (ProgressListener listener) {
        safeCallOfListener (listener, ProgressListener::onComputationStarted);
        safeCallOfListener (listener, ProgressListener::onStepComputed); // 0 step
        
        for (int i = Ws.size (); i < ITERATIONS; i = Ws.size ()) {
            double [][] equations = new double [2 * NODES][2 * NODES];
            double [] y = new double [2 * NODES];
            
            Double [] previousA = As.get (i - 1), previousT = Ts.get (i);
            double fW, fdW_dA, fdW_dT, fdW_dAa, fdW_dTt;
            
            // Left-side border condition
            fW      = W.apply     (previousA [0], previousT [0]);
            fdW_dA  = dW_dA.apply (previousA [0], previousT [0]);
            fdW_dT  = dW_dT.apply (previousA [0], previousT [0]);
            fdW_dAa = fdW_dA * previousA [0];
            fdW_dTt = fdW_dT * previousT [0];
            
            y [0] = previousA [0] / dt + fW - fdW_dAa - fdW_dTt;
            equations [0][0] = 1 / dt + 2 * D / dx2 - fdW_dA;
            equations [0][1] = -fdW_dT;
            equations [0][2] = -D / dx2;
            
            y [1] = ti * KAPPA / dx2 + previousT [0] / dt
                  + QC * (-fW + fdW_dAa + fdW_dTt);
            equations [1][0] = QC * fdW_dA;
            equations [1][1] = 1 / dt + 2 * KAPPA / dx2 + QC * fdW_dT;
            equations [1][3] = -KAPPA / dx2;
            
            // Inner cells
            for (int x = 1; x < NODES - 1; x++) {
                fW      = W.apply     (previousA [x], previousT [x]);
                fdW_dA  = dW_dA.apply (previousA [x], previousT [x]);
                fdW_dT  = dW_dT.apply (previousA [x], previousT [x]);
                fdW_dAa = fdW_dA * previousA [x];
                fdW_dTt = fdW_dT * previousT [x];
                
                y [2 * x + 0] = previousA [x] / dt + fW - fdW_dAa - fdW_dTt;
                equations [2 * x + 0][2 * x - 2] = -D / dx2;
                equations [2 * x + 0][2 * x + 0] = 1 / dt + 2 * D / dx2 - fdW_dA;
                equations [2 * x + 0][2 * x + 1] = -fdW_dT;
                equations [2 * x + 0][2 * x + 2] = -D / dx2;

                y [2 * x + 1] = previousT [x] / dt + QC * (-fW + fdW_dAa + fdW_dTt);
                equations [2 * x + 1][2 * x - 1] = -KAPPA / dx2;
                equations [2 * x + 1][2 * x + 0] = QC * fdW_dA;
                equations [2 * x + 1][2 * x + 1] = 1 / dt + 2 * KAPPA / dx2 + QC * fdW_dT;
                equations [2 * x + 1][2 * x + 3] = -KAPPA / dx2;
            }
            
            // Right-side border condition
            fW      = W.apply     (previousA [NODES - 1], previousT [NODES - 1]);
            fdW_dA  = dW_dA.apply (previousA [NODES - 1], previousT [NODES - 1]);
            fdW_dT  = dW_dT.apply (previousA [NODES - 1], previousT [NODES - 1]);
            fdW_dAa = fdW_dA * previousA [NODES - 1];
            fdW_dTt = fdW_dT * previousT [NODES - 1];
            
            y [2 * NODES - 2] = previousA [NODES - 1] / dt 
                              + fW - fdW_dAa - fdW_dTt;
            equations [2 * NODES - 2][2 * NODES - 4] = -D / dx2;
            equations [2 * NODES - 2][2 * NODES - 2] = 1 / dt + D / dx2 - fdW_dA;
            equations [2 * NODES - 2][2 * NODES - 1] = -fdW_dT;

            y [2 * NODES - 1] = KAPPA * T0 / dx2 + previousT [NODES - 1] / dt 
                              + QC * (-fW + fdW_dAa + fdW_dTt);
            equations [2 * NODES - 1][2 * NODES - 3] = -KAPPA / dx2;
            equations [2 * NODES - 1][2 * NODES - 2] = QC * fdW_dA;
            equations [2 * NODES - 1][2 * NODES - 1] = 1 / dt + 2 * KAPPA / dx2 + QC * fdW_dT;
            
            Double [] rA = new Double [NODES], rT = new Double [NODES],
                      rW = new Double [NODES];
            double [] result = MatrixSolver.solve (equations, y);
            for (int x = 0; x < NODES; x++) {
                rA [x] = result [2 * x + 0];
                rT [x] = result [2 * x + 1];
                
                rW [x] = abs (W.apply (rA [x], rT [x]));
            }
            As.add (rA); Ts.add (rT); Ws.add (rW);
            
            safeCallOfListener (listener, ProgressListener::onStepComputed);
        }
        
        safeCallOfListener (listener, ProgressListener::onComputationFinished);
    }
    
    public int computredSteps () {
        return Ws.size ();
    }
    
}