#!/bin/bash
./modelcount.sh -vars="methane,high_water,pump_on" "-ltl=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=(F((high_water & pump_on & X(high_water))) | (G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(!pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((G(!high_water) & G((!methane | X(!pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((F((high_water & X(!pump_on))) & G((!methane | X(!pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((methane & high_water & pump_on & X((high_water & X(!high_water))))))" "-ref=(F((methane & X(pump_on))) | F((high_water & X(!pump_on))) | F((high_water & pump_on & X((!high_water & X(high_water))))))" "-ref=((F((methane & X(pump_on))) & G((high_water | X(pump_on)))) | F((high_water & pump_on & X((!high_water | X(!high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(!high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | G((!high_water | !pump_on | X((!high_water | X(!high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & !high_water & X(high_water))))))" "-ref=((G(X(!pump_on)) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & G(high_water))))))" "-ref=((G((!methane | X(pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(!high_water))))))" "-ref=((G((!high_water | pump_on)) & G((!methane | X(!pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((!high_water | X(!high_water))))))" "-ref=((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(!high_water))))))" "-ref=((G(!high_water) & G((!methane | X(!pump_on)))) | F((high_water & pump_on & X(X(high_water)))))" "-ref=((G(methane) & G(X(pump_on)) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=((methane & G((!methane | X(!pump_on))) & G((!high_water | X(!pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water))))))" "-ref=(!high_water | (G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(!high_water))))))" "-out=result/minepump/minepump-prefix.out" -k=20 -prefixes 
