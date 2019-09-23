#!/opt/local/bin/runhugs
{-# LANGUAGE DeriveDataTypeable #-}
--Datatype for LTL*
import System.Environment
import Data.Char
import Control.Exception 
import Data.Set
import Data.List
import Data.Typeable
import System.IO

import Debug.Trace

data MyException = Error String deriving(Show,Typeable)

instance Exception MyException 

--LEXER

data TokenLTL = PROP String
  		       | TRUE
  		       | FALSE
             | NEG 
             | AND
             | OR
             | IMP
             | EQUIV
             | NEXT
             | UNTIL
             | WUNTIL
             | REL
             | MREL
             | DIAM
             | BOX
             | LPAR
             | RPAR
             deriving(Show)


lexLTL [] = []
lexLTL ('!':xr) = NEG:lexLTL xr
lexLTL  ('&':xr) = AND:lexLTL xr
lexLTL ('|':xr) = OR:lexLTL xr
lexLTL ('-':'>':xr) = IMP:lexLTL xr
lexLTL ('<':'-':'>':xr) = EQUIV:lexLTL xr
lexLTL ('X':xr) = NEXT:lexLTL xr
lexLTL ('U':xr) = UNTIL:lexLTL xr
lexLTL ('W':xr) = WUNTIL:lexLTL xr
lexLTL ('R':xr) = REL:lexLTL xr
lexLTL ('M':xr) = MREL:lexLTL xr
lexLTL ('F':xr) = DIAM:lexLTL xr
lexLTL ('G':xr) = BOX:lexLTL xr
lexLTL  (' ':xr) = lexLTL xr
lexLTL ('\t':xr) = lexLTL xr
lexLTL ('\n':xr) = lexLTL xr
lexLTL ('(':xr) = LPAR: lexLTL xr
lexLTL (')':xr) = RPAR: lexLTL xr
lexLTL ('t':'r':'u':'e':xr) = TRUE: lexLTL xr
lexLTL ('f':'a':'l':'s':'e':xr) = FALSE: lexLTL xr
lexLTL (c:xr) = if isAlpha c then lexProp [c] xr
    	        else error "lex"


lexProp cs cs' = if Data.List.null cs' || not(isAlphaNum (head cs')) then PROP(reverse cs):lexLTL cs'
	             else lexProp (head cs' : cs) (tail cs')

--PARSER

data ExpLTL a = Prop a
		   | T
		   | F	
       | Neg  (ExpLTL a)
		   | And  (ExpLTL a, ExpLTL a)
		   | Or   (ExpLTL a, ExpLTL a)
		   | Imp  (ExpLTL a, ExpLTL a)
		   | Equiv(ExpLTL a, ExpLTL a)
		   | Next (ExpLTL a)
		   | Until(ExpLTL a, ExpLTL a)
       | WUntil(ExpLTL a, ExpLTL a)
		   | Rel  (ExpLTL a, ExpLTL a)
       | MRel (ExpLTL a, ExpLTL a)
		   | Diam (ExpLTL a)
		   | Box  (ExpLTL a) 
		   deriving(Show, Eq)

leftChild (Neg e) = e
leftChild (And(e,e')) = e
leftChild (Or(e,e')) = e
leftChild (Imp(e,e')) = e
leftChild (Equiv(e,e')) = e
leftChild (Next e) = e
leftChild (Until(e,e')) = e
leftChild (WUntil(e,e')) = e
leftChild (Rel(e,e')) = e
leftChild (MRel(e,e')) = e
leftChild (Diam e) = e
leftChild (Box e) = e
leftChild _ = throw (Error "No left child")


rightChild (And(e,e')) = e'
rightChild (Or(e,e')) = e'
rightChild (Imp(e,e')) = e'
rightChild (Equiv(e,e')) = e'
rightChild (Until(e,e')) = e'
rightChild (WUntil(e,e')) = e'
rightChild (Rel(e,e')) = e'
rightChild (MRel(e,e')) = e'
rightChild _ =  throw (Error "No right child")

foldLTL :: (ExpLTL a-> b)-> ((ExpLTL a,[b])->b)-> ExpLTL a-> b
foldLTL f g (T)           = f T
foldLTL f g (F)           = f F
foldLTL f g (Prop s)      = f (Prop s)
foldLTL f g (Neg e)       = g(Neg e, [foldLTL f g e])
foldLTL f g (And(e,e'))   = g(And(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Or(e,e'))    = g(Or(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Imp(e,e'))   = g(Imp(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Equiv(e,e')) = g(Equiv(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Next e)      = g(Next(e),[foldLTL f g e])
foldLTL f g (Until(e,e')) = g(Until(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (WUntil(e,e')) = g(WUntil(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Rel(e,e'))   = g(Rel(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (MRel(e,e'))   = g(MRel(e,e'),[foldLTL f g e, foldLTL f g e'])
foldLTL f g (Diam e)      = g(Diam(e),[foldLTL f g e])
foldLTL f g (Box e)       = g(Box(e),[foldLTL f g e])


parseLTL2 ts = case (impexp ts) of
					(e, EQUIV:tr) -> let (e',tr') = parseLTL2 tr in (Equiv(e,e'), tr')
					s -> s 
impexp   ts = case (orexp ts)  of
	                (e, IMP:tr) -> let (e',tr') = impexp tr in (Imp(e,e'), tr')
	                s -> s
orexp    ts =  orexp' (andexp ts)
orexp' (e, OR:tr) = orexp' (let (e', tr') = andexp tr in (Or(e,e'), tr'))
orexp' s = s
andexp   ts = andexp' (temp1exp ts)
andexp'(e,AND:tr) = andexp' (let (e', tr') = temp1exp tr in (And(e,e'), tr'))
andexp' s = s
temp1exp ts = case (temp2exp ts) of
                   (e, UNTIL:tr)   ->  let (e',tr') = temp1exp tr in (Until(e,e'), tr')
                   (e, WUNTIL:tr)  ->  let (e',tr') = temp1exp tr in (WUntil(e,e'), tr')
                   (e, REL:tr)     -> let (e',tr') = temp1exp tr in (Rel(e,e'), tr')
                   (e, MREL:tr)     -> let (e',tr') = temp1exp tr in (MRel(e,e'), tr')
                   s -> s
temp2exp ts = case ts of
                   (BOX:tr) -> let (e', tr') = temp1exp tr in (Box(e'),tr')
                   (DIAM:tr)-> let (e', tr') = temp1exp tr in (Diam(e'),tr')
                   (NEXT:tr) -> let (e', tr') = temp2exp tr in (Next(e'),tr')
                   (NEG:tr) -> let (e', tr') = temp2exp tr in (Neg(e'),tr')
                   s -> pexp s
pexp (PROP s:tr) = (Prop s, tr)
pexp (TRUE:tr) = (T, tr)
pexp (FALSE:tr) = (F, tr)
pexp (LPAR: tr) = case parseLTL2 tr of
                        (e, RPAR:tr') -> (e,tr')
                        _ -> error "Missing closing Right Paranthesis"
pexp _ = error "Syntax Error: Missing PEXP"                       	             


parseLTL ts = let (e, tr) = parseLTL2 ts in if Data.List.null tr then e else error "Syntax Error: Wrong Ending"

--Transfer to normal form
-- normal form ~, /\, U, O, F, T

normalForm :: ExpLTL a -> ExpLTL a
normalForm (Prop s) = Prop s
normalForm (Neg e) = Neg (normalForm e)
normalForm (And(e,e')) = And(normalForm e, normalForm e')
normalForm (Or(e,e')) = Neg(And(normalForm (Neg e), normalForm (Neg e')))
normalForm (Imp(e,e')) = Neg(And(normalForm e, normalForm (Neg e')))
normalForm (Equiv(e,e')) = And( Neg(And(normalForm e, normalForm (Neg e'))),Neg(And(normalForm e', normalForm (Neg e))))
normalForm (Next e) = Next(normalForm e)
normalForm (Until(e,e')) = Until(normalForm e, normalForm e')
normalForm (WUntil(e,e')) = normalForm (Or(Box (e), Until(e, e')))
normalForm (Rel(e,e')) = Neg (Until(normalForm (Neg e), normalForm (Neg e')))
normalForm (MRel(e,e')) = normalForm (Neg (WUntil(Neg e, Neg e')))
normalForm (Diam e) = Until(T, normalForm e)
normalForm (Box e) = Neg (Until(T, normalForm (Neg e)))
--normalForm s = s

--positive normal form
pnf :: ExpLTL a -> ExpLTL a
--pnf (FALSE) = FALSE
pnf (Prop s) = Prop s
pnf (Neg e) =  negPnf e
pnf (And(e,e')) = And(pnf e, pnf e')
pnf (Or(e,e')) = Or(pnf e, pnf e')
pnf (Imp(e,e')) = Imp(pnf e, pnf e')
pnf (Equiv(e,e')) = Equiv(pnf e, pnf e')
pnf (Next e) = Next(pnf e)
pnf (Until(e,e')) = Until(pnf e, pnf e')
pnf (WUntil(e,e')) = pnf (Or(Box (e), Until(e, e')))
pnf (Rel(e,e')) = Rel(pnf e, pnf e')
pnf (MRel(e,e')) = pnf (Neg (WUntil(Neg e, Neg e')))
pnf (Diam e) = Until(T, pnf e)
pnf (Box e) = Rel(F, pnf e)
pnf (x) = x 


negPnf :: ExpLTL a -> ExpLTL a
negPnf (Prop s) = Neg(Prop s)
negPnf (Neg e) =  pnf e
negPnf (And(e,e')) = Or(negPnf e, negPnf e')
negPnf (Or(e,e')) = And(negPnf e, negPnf e')
negPnf (Imp(e,e')) = And(pnf e, negPnf e')
negPnf (Equiv(e,e')) = Or(And(pnf e, negPnf e'), And(negPnf e, pnf e'))
negPnf (Next e) = Next(negPnf e)
negPnf (Until(e,e')) = Rel(negPnf e, negPnf e')
negPnf (WUntil(e,e')) = negPnf (Or(Box (e), Until(e, e')))
negPnf (Rel(e,e')) = Until(negPnf e, negPnf e')
negPnf (MRel(e,e')) = negPnf (Neg (WUntil(Neg e, Neg e')))
negPnf (Diam e) = Rel(F, negPnf e)
negPnf (Box e) = Until(T,negPnf e)



elimDoubleNegation :: ExpLTL a -> ExpLTL a
elimDoubleNegation (Neg(Neg e)) = elimDoubleNegation e
elimDoubleNegation (Prop s) = Prop s
elimDoubleNegation (Neg e) = Neg (elimDoubleNegation e)
elimDoubleNegation (And(e,e')) = And(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Or(e,e')) = Or(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Imp(e,e')) = Imp(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Equiv(e,e')) = Equiv(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Next e) = Next(elimDoubleNegation e)
elimDoubleNegation (Until(e,e')) = Until(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (WUntil(e,e')) = WUntil(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Rel(e,e')) = Rel(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (MRel(e,e')) = MRel(elimDoubleNegation e, elimDoubleNegation e')
elimDoubleNegation (Diam e) = Diam(elimDoubleNegation e)
elimDoubleNegation (Box e) = Box(elimDoubleNegation e)
elimDoubleNegation s = s

--LTL2GNBA
-- Computes closure of LTL formula: CAUTION: the procedure assumes that formula is in pnf (negForm)
isIn x (y:ys) = if x == y then True else isIn x ys
isIn x [] = False

unify xs [] = xs
unify [] ys = ys
unify (x:xs) ys = if isIn x ys then unify xs ys else unify xs (x:ys)

subFormulas :: Eq a => ExpLTL a-> [ExpLTL a]
subFormulas formula = case (formula) of 
              	                        T             -> [T]
              	                        F             -> [F]
              	                        Prop s        -> [Prop s]
              	                        Neg e         -> let xs = subFormulas e in if isIn (Neg e) xs then xs else Neg e:xs
              	                        And (e,e')    -> let xs = (subFormulas e `unify` subFormulas e') in if isIn (And(e,e')) xs then xs else And(e,e'):xs
              	                        Next e        -> let xs = subFormulas e in if isIn (Next e) xs then xs else Next e:xs
              	                        Until (e,e')  -> let xs = (subFormulas e `unify` subFormulas e') in if isIn (Until(e,e')) xs then xs else Until(e,e'):xs
                                        WUntil (e,e') -> let xs = (subFormulas e `unify` subFormulas e') in if isIn (WUntil(e,e')) xs then xs else WUntil(e,e'):xs
              	                        _             -> error "Convert to normal form using normalForm"

--No negations
pureSubFormulas :: Eq a => ExpLTL a-> [ExpLTL a]
pureSubFormulas formula = case formula of 
              	                        T -> [T]
              	                        F -> [F]
              	                        Prop s -> [Prop s]
              	                        Neg e -> pureSubFormulas e
              	                        And (e,e') -> let xs = (pureSubFormulas e `unify` pureSubFormulas e') in if isIn (And(e,e')) xs then xs else And(e,e'):xs
              	                        Next e -> let xs = pureSubFormulas e in if isIn (Next e) xs then xs else Next e:xs
              	                        Until (e,e') -> let xs = (pureSubFormulas e `unify` pureSubFormulas e') in if isIn (Until(e,e')) xs then xs else Until(e,e'):xs
                                        WUntil (e,e') -> let xs = (pureSubFormulas e `unify` pureSubFormulas e') in if isIn (WUntil(e,e')) xs then xs else WUntil(e,e'):xs
              	                        _ -> error "Convert to normal form using normalForm"


--closureLTL :: Eq a => ExpLTL a->[(ExpLTL a, ExpLTL a)]
closureLTL formula = let subForm = subFormulas formula
                         negations = Data.List.foldr (\ x s -> case x of 
                         	                                        F -> (T,F):s 
                         	                                        T -> (T,F):s
                         	                                        (Neg x') -> (x',Neg x'):s
                         	                                        _ -> (x,Neg x):s
                         	                         ) [] subForm
                      in negations
 

-- Constructs elementary sets: CAUtION: assumes that a formula and its negation have the same index in the closure lists
elementarySets :: Eq a => [ExpLTL a] -> [[ExpLTL a]]
elementarySets [] = []
elementarySets [x] = case x of 
	                      Neg x' -> [[x'],[x]]
	                      x' -> [[x'],[Neg x']]
elementarySets (x:xs) = let 
                           px = case x of 
                           	         (Neg x') -> x'
                           	         _ -> x
                           nx = case x of 
                           	         T -> F
                           	         F -> T
                           	         (Neg x') -> x
                           	         _ -> Neg x
                           elemSets = elementarySets xs
                           check form set = if isIn form set then False 
                           	                 else
                                                  case form of
                           	                      (And(e,e')) -> if (isIn e set) && (isIn e' set) then True else False
                           	                      T -> if (isIn T set) then False else True
                           	                      (Until(e,e')) -> if (isIn e' set) then True
                           	                      	                else if (isIn e set) then True else False
                                                  --(WUntil(e,e')) -> if (isIn e' set) then True
                                                  --                  else if (isIn e set) then True else False
                           	                      e-> case e of 
                           	                      	       (Neg(Until(e,e')))->if (isIn e' set) then False else True
                           	                      	       (Neg(And(e,e')))-> if (isIn e set) && (isIn e' set) then False else True
                           	                      	       (Neg e') -> if (isIn e' set) then False else True
                           	                      	       _ -> if (isIn (Neg e) set) then False else True 
                           elemSets_px = if px == F then [] else Data.List.foldl (\ s set -> if check px set then (px:set):s else s) [] elemSets  
                           elemSets_nx = if nx == F then [] else Data.List.foldl (\ s set -> if check nx set then (nx:set):s else s) [] elemSets  
                         in
                           elemSets_px ++ elemSets_nx   



--GNBA


-- SAT Formulas 
data ExpSAT  =  PF
               | PT 
               | PPVar String
               | PNVar String
               | PNeg (ExpSAT )
               | PAnd (ExpSAT ) (ExpSAT )
               | POr (ExpSAT ) (ExpSAT ) 
               | PImp (ExpSAT ) (ExpSAT )
               | PEquiv (ExpSAT ) (ExpSAT )
               deriving(Show,Eq)

nnf :: ExpSAT -> ExpSAT
nnf (PF) = PF
nnf (PT) = PT
nnf (PPVar a) = PPVar a
nnf (PNVar a) = PNVar a
nnf (PNeg  expr) = case expr of 
                          PF -> PT
                          PT -> PF
                          PPVar a -> PNVar a
                          PNVar a -> PPVar a
                          PNeg  e -> nnf e
                          PAnd e e' -> POr (nnf (PNeg e)) (nnf (PNeg e'))
                          POr e e' -> PAnd (nnf (PNeg e)) (nnf (PNeg e'))
                          PImp e e' -> PAnd (nnf e) (nnf (PNeg e'))
                          PEquiv e e' -> POr (nnf (PAnd e (PNeg e'))) (nnf (PAnd (PNeg e) e'))
                          
nnf (PAnd e e') = PAnd (nnf e) (nnf e')
nnf (POr e e')  = POr (nnf e) (nnf e')
nnf (PImp e e') = POr (nnf (PNeg e)) (nnf e')
nnf (PEquiv e e') = nnf (PAnd (PImp e e') (PImp e' e))

deleteEquiv :: ExpSAT -> ExpSAT
deleteEquiv expr = case expr of 
                          PEquiv e e' -> PAnd (PImp e e') (PImp e' e)
                          PF -> PF
                          PT -> PT
                          PPVar a -> PPVar a
                          PNVar a -> PNVar a
                          PNeg  e -> PNeg (deleteEquiv e)
                          PAnd e e' -> PAnd (deleteEquiv e) (deleteEquiv e')
                          POr e e' -> POr (deleteEquiv e) (deleteEquiv e')   
                          PImp e e' -> PImp (deleteEquiv e) (deleteEquiv e')

deleteImp :: ExpSAT -> ExpSAT
deleteImp expr = case expr of 
                          PF -> PF
                          PT -> PT
                          PPVar a -> PPVar a
                          PNVar a -> PNVar a
                          PNeg  e -> PNeg (deleteImp e)
                          PAnd e e' -> PAnd (deleteImp e) (deleteImp e')
                          POr e e' -> POr (deleteImp e) (deleteImp e')   
                          PImp e e' -> POr (PNeg (deleteImp e)) (deleteImp e')
                          _ -> error "deleteImp assumes that Equiv have been previously removed."

pushNegation :: ExpSAT -> ExpSAT
pushNegation (PF) = PF
pushNegation (PT) = PT
pushNegation (PPVar a) = PPVar a
pushNegation (PNVar a) = PNVar a
pushNegation (PNeg  expr) = case expr of 
                          PF -> PT
                          PT -> PF
                          PPVar a -> PNVar a
                          PNVar a -> PPVar a
                          PNeg  e -> pushNegation e
                          PAnd e e' -> POr (pushNegation (PNeg e)) (pushNegation (PNeg e'))
                          POr e e' -> PAnd (pushNegation (PNeg e)) (pushNegation (PNeg e'))
                          _ -> error "deleteImp assumes that Imp and Equiv have been previously removed."
pushNegation (PAnd e e') = PAnd (pushNegation e) (pushNegation e')
pushNegation (POr e e') = POr (pushNegation e) (pushNegation e')
pushNegation (PImp _ _) = error "deleteImp assumes that Imp has been previously removed."
pushNegation (PEquiv _ _) = error "deleteImp assumes that Equiv has been previously removed."

distCD :: ExpSAT -> ExpSAT
distCD (PAnd e e') = PAnd (distCD e) (distCD e')
distCD (POr e e')  =  let new_e = distCD e ;
                          new_e' = distCD e' 
                      in
                        case new_e of
                          PAnd a a' -> PAnd (distCD (POr a new_e')) (distCD (POr a' new_e'))
                          _ -> case new_e' of 
                                  PAnd a a' -> PAnd (distCD (POr new_e a)) (distCD (POr new_e a'))
                                  _ -> POr new_e new_e'
distCD (PNeg e) = error "distCD assumes that PNeg has been previously removed."
distCD x = x

removeTrueFalse :: ExpSAT -> ExpSAT
removeTrueFalse (PAnd PT e') = removeTrueFalse e'
removeTrueFalse (PAnd e PT) = removeTrueFalse e
removeTrueFalse (PAnd PF e') = PF
removeTrueFalse (PAnd e PF) = PF
removeTrueFalse (PAnd (PPVar a) (PNVar a')) = if (a == a') then PF else (PAnd (PPVar a) (PNVar a'))
removeTrueFalse (PAnd (PNVar a) (PPVar a')) = if (a == a') then PF else (PAnd (PNVar a) (PPVar a'))
removeTrueFalse (PAnd e e') = PAnd (removeTrueFalse e) (removeTrueFalse e')
removeTrueFalse (POr PT e') = PT
removeTrueFalse (POr e PT) = PT
removeTrueFalse (POr PF e') = removeTrueFalse e'
removeTrueFalse (POr e PF) = removeTrueFalse e
removeTrueFalse (POr (PPVar a) (PNVar a')) = if (a == a') then PT else (POr (PPVar a) (PNVar a'))
removeTrueFalse (POr (PNVar a) (PPVar a')) = if (a == a') then PT else (POr (PNVar a) (PPVar a'))
removeTrueFalse (POr e e') = POr (removeTrueFalse e) (removeTrueFalse e') 
removeTrueFalse x = x


cnf :: ExpSAT -> ExpSAT
cnf expr = removeTrueFalse (distCD (pushNegation (deleteImp (deleteEquiv expr))))

splitCNFclauses :: ExpSAT -> [ExpSAT]
splitCNFclauses (PAnd e e') = (splitCNFclauses e) ++ (splitCNFclauses e')
splitCNFclauses x = [x]

sat2string expr = case expr of 
                        PPVar a -> a
                        PNVar a -> "!"++(a)
                        PNeg  e -> "!"++(sat2string e)
                        PAnd e e' -> "("++ (sat2string e) ++ ")"++"&"++"("++ (sat2string e') ++")"
                        POr e e' -> "("++ (sat2string e) ++ ")"++"|"++"("++ (sat2string e') ++")"
                        PImp e e' -> "("++ (sat2string e) ++ ")"++"->"++"("++ (sat2string e') ++")"
                        PEquiv e e' -> "("++ (sat2string e) ++ ")"++"="++"("++ (sat2string e') ++")"
                        PF -> "0"
                        PT -> "1"

evalTrueFalse expr = case expr of 
                        PNeg  e -> let e' = evalTrueFalse e in if e'==PF then PT else if e' == PT then PF else PNeg e'
                        PAnd e e' -> let e1 = evalTrueFalse e
                                         e2 = evalTrueFalse e'
                                      in
                                       if (e1==PF || e2==PF) then PF else if e1==PT then e2 else if e2==PT then e1 else PAnd e1 e2
                        POr e e' -> let e1 = evalTrueFalse e
                                        e2 = evalTrueFalse e'
                                      in
                                       if (e1==PT || e2==PT) then PT else if e1==PF then e2 else if e2==PF then e1 else POr e1 e2
                        PImp e e' -> let e1 = evalTrueFalse e
                                         e2 = evalTrueFalse e'
                                      in
                                       if e2==PT then PT else if e1==PF then PT else if e1==PT then e2 else if e2==PF then e1 else PImp e1 e2
                        PEquiv e e' -> let e1 = evalTrueFalse e
                                           e2 = evalTrueFalse e'
                                      in
                                       case (e1,e2) of
                                             (PF,PF)-> PT
                                             (PF,PT)-> PF
                                             (PT,PF)-> PF
                                             (PT,PT)-> PT
                                             (PF, _)-> PNeg e2
                                             ( _,PF)-> PNeg e1
                                             (PT, _)-> e2
                                             ( _,PT)-> e1
                                             ( _, _)-> PEquiv e1 e2

                        e -> e 



-- encode word model counting in SAT

-- Given a bound k the formula wordModel encodes the universal language of words of length k. NOTICE: Not needed as the universal language is encoded by TRUE? 

-- Given a bound k and an a set of atomic propositions ap, the formula loop encodes the loop position in the word model by forcing that the assignment of the kth position be equal to the loop position. 
-- It also states that there is exaclty one loop along the word model

loop k ap = PAnd (atLeastOneLoop k ap) (atMostOneLoop k)

atLeastOneLoop k ap = if k<=0 then error "Bound not in Range" else PAnd (atLeastOneLoopLeft k ap) (atLeastOneLoopRight k)
atLeastOneLoopLeft k ap = atLeastOneLoopLeft' (k-1) k ap
atLeastOneLoopLeft' :: (Show a, Num a, Eq a) => a -> a -> [String] -> ExpSAT
atLeastOneLoopLeft' 0 k ap = PImp (PPVar ("l"++(show 0))) 
                                  (allPropsEqual 0 k ap)
atLeastOneLoopLeft' c k ap = PAnd (PImp (PPVar ("l"++(show c))) 
                                        (allPropsEqual c k ap)
                                  ) 
                                  (atLeastOneLoopLeft' (c-1) k ap)

allPropsEqual :: (Show a, Num a, Eq a) => a -> a -> [String] -> ExpSAT
allPropsEqual a b [] = error "empty set of atomic propositions"
allPropsEqual a b [p] = PEquiv (PPVar (( p)++(show a))) 
                               (PPVar (( p)++(show b)))
allPropsEqual a b (p:ap) = PAnd (PEquiv (PPVar (( p)++(show a))) 
                                        (PPVar (( p)++(show b)))
                                ) 
                                (allPropsEqual a b ap)

atLeastOneLoopRight :: (Show a, Num a, Eq a) =>  a -> ExpSAT 
atLeastOneLoopRight 1 = PPVar ("l"++(show 0))
atLeastOneLoopRight k = POr (PPVar ("l"++(show (k-1)))) 
                               (atLeastOneLoopRight (k-1))

atMostOneLoop :: (Show a, Num a, Eq a) => a -> ExpSAT 
atMostOneLoop 0 = error "atMostOneLoop: not word of size k = 0"
atMostOneLoop 1 = PImp (smallerExists 0) 
                       (PNVar ("l"++(show 0)))
atMostOneLoop k = PAnd (PImp (smallerExists (k-1))
                             (PNVar ("l"++(show (k-1))))
                       ) 
                       (atMostOneLoop (k-1))

smallerExists :: (Show a, Num a, Eq a) => a -> ExpSAT 
smallerExists 0 = PF
smallerExists k = POr (PPVar ("l"++(show (k-1))))
                      (smallerExists (k-1))


-- TODO Solve show String problem and indecies check

--Given an LTL formula phi and an index i the formula fixpoint encodes the LTL formula phi. Notice that phi must be given in pnf

fixpoint :: (Show b, Ord b, Num b)=> ExpLTL String -> b -> b -> ExpSAT 
fixpoint phi i k = case phi of 
                     Prop a -> fixpointProp phi i k
                     T -> PT
                     F -> PF 
                     Neg e -> fixpointNProp phi i k
                     And (e, e') -> PAnd (fixpoint e i k) (fixpoint e' i k)
                     Or (e, e') -> POr (fixpoint e i k) (fixpoint e' i k)
                     Imp (e, e') -> PImp (fixpoint e i k) (fixpoint e' i k)
                     Equiv (e, e') -> PEquiv (fixpoint e i k) (fixpoint e' i k)
                     Next e -> fixpointNext phi i k
                     Until (e, e') -> fixpointUntil phi i k
                     WUntil (e, e') -> fixpoint (Or(Box (e), Until(e, e'))) i k
                     Rel (e, e') -> fixpointRel phi i k 
                     MRel (e, e') -> fixpointRel (Neg (WUntil(Neg e, Neg e'))) i k 
                     _ -> error "fixpoint: Transform formula to pnf"

fixpointProp :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT 
fixpointProp (Prop p) i k = if i < k then PPVar ((p)++(show i))
                             else if i == k then fixpointProp' (Prop p) k
                                   else error "fixpointProp: k<i"

fixpointProp' :: (Show b, Ord b, Num b) => ExpLTL String -> b -> ExpSAT 
fixpointProp' (Prop p) 0 = error "fixpointProp': no word of size k = 0"
fixpointProp' (Prop p) 1 = PAnd (PPVar ("l"++(show 0))) (PPVar (( p)++(show 0)))
fixpointProp' (Prop p) k = POr (PAnd (PPVar ("l"++(show (k-1)))) (PPVar (( p)++(show (k-1)))))
                               (fixpointProp' (Prop p) (k-1))

fixpointNProp :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT 
fixpointNProp (Neg(Prop p)) i k = if i < k then PNVar (( p)++(show i))
                                   else if i == k then fixpointNProp' (Neg(Prop p)) k
                                         else error "fixpointNProp: k<i"

fixpointNProp' :: (Show b, Ord b, Num b) => ExpLTL String -> b -> ExpSAT 
fixpointNProp' (Prop p) 0 = error "fixpointNProp': no word of size k = 0"
fixpointNProp' (Prop p) 1 = PAnd (PPVar ("l"++(show 0))) (PNVar (( p)++(show 0)))
fixpointNProp' (Prop p) k = POr (PAnd (PPVar ("l"++(show (k-1)))) (PNVar (( p)++(show (k-1)))))
                                     (fixpointNProp' (Prop p) (k-1))

fixpointNext :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointNext (Next e) i k = if i < (k-1) then fixpoint e (i+1) k 
                             else if i == (k-1) then fixpointNext' e k k
                                   else error "fixpointNext: k<i"

fixpointNext' :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointNext' e _ 0 = error "fixpointNext': no word has size k = 0"
fixpointNext' e 1 k = PAnd (PPVar ("l"++(show 0))) (fixpoint e 0 k)
fixpointNext' e k' k = POr (PAnd (PPVar ("l"++(show (k'-1)))) (fixpoint e (k'-1) k))
                           (fixpointNext' e (k'-1) k)


fixpointUntil :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointUntil (Until(e, e')) i k = if i < k then POr (fixpoint e' i k) (PAnd (fixpoint e i k) (fixpoint (Until(e, e')) (i+1) k))
                                    else if i == k then fixpointUntil' (Until(e, e')) k k 
                                          else error "fixpointUntil: k<i"

fixpointUntil' :: ( Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointUntil' e _ 0 = error "fixpointUntil': no word has size k = 0"
fixpointUntil' e 1 k = PAnd (PPVar ("l"++(show 0))) (fixpointUntil2 e 0 k)
fixpointUntil' e k' k = POr (PAnd (PPVar ("l"++(show (k'-1)))) (fixpointUntil2 e (k'-1) k))
                            (fixpointUntil' e (k'-1) k) 

fixpointUntil2 :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointUntil2 (Until(e,e')) i k = if i == k then PF 
                        else POr (fixpoint e' i k) 
                                 (PAnd (fixpoint e i k) (fixpointUntil2 (Until(e,e')) (i+1) k )) 



fixpointRel :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointRel (Rel(e, e')) i k = if i < k then PAnd (fixpoint e' i k) (POr (fixpoint e i k) (fixpoint (Rel(e, e')) (i+1) k))
                                    else if i == k then fixpointRel' (Rel(e, e')) k k 
                                          else error "fixpointRel: k<i"

fixpointRel' :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointRel' e _ 0 = error "fixpointRel': no word has size k = 0"
fixpointRel' e 1 k = PAnd (PPVar ("l"++(show 0))) (fixpointRel2 e 0 k)
fixpointRel' e k' k = POr (PAnd (PPVar ("l"++(show (k'-1)))) (fixpointRel2 e (k'-1) k))
                            (fixpointRel' e (k'-1) k) 

fixpointRel2 :: (Show b, Ord b, Num b) => ExpLTL String -> b -> b -> ExpSAT
fixpointRel2 (Rel(e,e')) i k = if i == k then PT
                        else PAnd (fixpoint e' i k) 
                                 (POr (fixpoint e i k) (fixpointRel2 (Rel(e,e')) (i+1) k )) 





-- Given an LTL formula phi and a bound k the following procedure computes a SAT formula that encodes all word models of phi of size k
bltl2sat :: ExpLTL String -> [String] -> Int -> ExpSAT
bltl2sat phi ap k = let pnfPhi = pnf phi
                        in
                        evalTrueFalse (PAnd (loop k ap)
                             (fixpoint pnfPhi 0 k))



--This part provides procedure to read a list of LTL formulas and write their SAT encodings into txt files
extractAlphabet [] = error "extractAlphabet: No Formulas"
--extractAlphabet ("###*":tr) = ([],tr)
extractAlphabet (a:tr) =  if (Data.List.isPrefixOf "###" a) then
                              ([],tr)
                          else
                            if (Data.List.isPrefixOf "--" a) then
                              extractAlphabet (tr)
                            else
                              let (alph,xs) = extractAlphabet tr
                              in 
                                --(trace ("alph = " ++ show a)) 
                              (a:alph,xs)

makeLTLList :: String -> IO ([ExpLTL String],[String])
makeLTLList path = do  
                     fileString <- readFile path 
                     let contentList = lines fileString
                     let (alphabet, formsStr) = extractAlphabet contentList
                     let formulaStringList = Data.List.filter (\l -> not $ Data.List.isPrefixOf "--" l) formsStr
                     let formulaList = Data.List.foldr (\ f s -> (parseLTL (lexLTL f)):s) [] formulaStringList 
                     return  (formulaList, alphabet)
 


unfoldBC :: ExpLTL String -> Int -> ExpLTL String
unfoldBC _ 0 = T
unfoldBC bc 1 = bc
unfoldBC bc n = let next = unfoldBC bc (n-1) ;
                    curr = Neg bc
                in
                  And (curr, Next next)

genCNFClauses :: [ExpSAT] -> String
genCNFClauses [] = ""
genCNFClauses (x:xs) = (sat2string x) ++ "\n" ++ (genCNFClauses xs)

main = do
         args <- getArgs;
         infile <- return $ args!!0 ;
         outfile <- return $ args!!1 ;
         boundS <- return $ args!!2 ;
         bound <- return $ Data.List.foldl (\ s d -> s*10 + (ord d)-(ord '0')) 0 boundS ;
         (formulas, alphabet) <- makeLTLList infile ;
         form <- return $ (Data.List.head formulas) ;
         if length formulas == 2 then
            let bc = unfoldBC (Data.List.last formulas) bound ;
                phi = And (form,bc) ;
                toSAT = bltl2sat phi alphabet bound ;
                --cnfSAT = cnf toSAT ;
                satString = (sat2string toSAT) 
            in
              --(trace ("bc " ++ show bc))
              writeFile (outfile++"-k"++(show bound)++".sat") satString 
         else
            let toSAT = (bltl2sat form alphabet bound) ;
                satString = (sat2string toSAT)
                --cnfSAT = cnf toSAT ;
                --cnfClauses = splitCNFclauses cnfSAT ;
                --satString = genCNFClauses cnfClauses 
            in
              writeFile (outfile++"-k"++(show bound)++".sat") satString 
        
         putStrLn "Done"











