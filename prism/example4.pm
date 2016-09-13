mdp

const double a00=0;
const double a01=1/5;
const double a10=1/7;
const double a11=0;
const double a12=1/13;



const double b00=0;
const double b01=1/19;
const double b10=1/21;
const double b11=0;
const double b12=1/29;

module example4

             s : [0..3] init 0;

             [alpha] s = 0 -> a00 : (s' = 0) + a01 : (s'=1) + 1-a00-a01 : (s'=3);
             [beta]  s = 0 -> b00 : (s' = 0) + b01 : (s'=1) + 1-b00-b01 : (s'=3);

             [alpha] s = 1 -> a10 : (s' = 0) + a11 : (s'=1) + a12: (s'=2) + 1-a10-a11-a12 : (s'=3);
             [beta]  s = 1 -> b10 : (s' = 0) + b11 : (s'=1) + b12: (s'=2) + 1-b10-b11-b12 : (s'=3);

             [alpha] s = 2 -> 1: (s' = 2);
             [alpha] s = 3 -> 1 : (s' = 3);

endmodule