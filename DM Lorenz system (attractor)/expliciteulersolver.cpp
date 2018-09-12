#include "expliciteulersolver.h"

ExplicitEulerSolver::ExplicitEulerSolver(double x0, double y0, double z0, double sigma, double r, double b, double dt)
    : SystemSolver (x0, y0, z0, sigma, r, b, dt)
{

}

std::vector<double> *ExplicitEulerSolver::solve(double time) {
    std::vector<double> *triple = new std::vector<double> [DIMENSION];
    for (int i = 0; i < DIMENSION; i++) {
        triple [i] = std::vector<double> ();
    }

    triple [0].push_back(x0); triple [1].push_back(y0); triple [2].push_back(z0);

    double currtentTime = 0;
    unsigned int gen = 0;

    while (currtentTime < time - dt) {
        double xk = triple [0][gen], yk = triple [1][gen], zk = triple [2][gen];

        triple [0].push_back(xk + dt * sigma * (yk - xk));
        triple [1].push_back(yk + dt * (xk * (r - zk) - yk));
        triple [2].push_back(zk + dt * (xk * yk - b * zk));

        currtentTime += dt;
        gen += 1;
    }


    return triple;
}

ExplicitEulerSolver::~ExplicitEulerSolver() {}