mdp

module example2

             s : [0..2] init 0;

             [alpha] s = 0 -> 1/2 : (s' = 0) + 1/3 : (s'=1) + 1/6 : (s'=2);
             [beta]   s = 0 -> 1/3 : (s' = 0) + 1/6 : (s'=1) + 1/2 : (s'=2);

             [alpha] s=1 -> 1.0 : (s'=1);
             [alpha] s=2 -> 1.0 : (s'=2);

endmodule
