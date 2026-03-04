(* Determiniatic Finite Automata *)

module TransMap = Map.Make (struct
    type t = int * char option

    let compare (s1, c1) (s2, c2) =
      let c = compare s1 s2 in
      if c <> 0 then c else compare c1 c2
    ;;
  end)

type fa =
  { states : int list
  ; accepting : int list
  ; start : int
  ; delta : int TransMap.t
  }

let accepts fa word =
  let rec run_fa_ s = function
    | [] -> List.mem s fa.accepting
    | x :: xs ->
      let s_ = TransMap.find_opt (s, Some x) fa.delta in
      (match s_ with
       | None -> false
       | Some state -> run_fa_ state xs)
  in
  run_fa_ fa.start word
;;

type nfa =
  { states : int list
  ; accepting : int list
  ; start : int
  ; delta : int list TransMap.t
  }

type re =
  | Empty
  | Epsilon
  | Char of char
  | Union of re * re
  | Concat of re * re
  | Star of re

let union nfa1 nfa2 = None
(* 
       rename states in nfa2
       merge states
       hook up nfa1 (accepting?) to nfa2
       merge deltas
   *)

let rec re_to_nfa = function
  | Empty -> { states = [ 0 ]; start = 0; accepting = []; delta = TransMap.empty }
  | Epsilon ->
    { states = [ 0 ]
    ; start = 0
    ; accepting = [ 0 ]
    ; delta = TransMap.empty |> TransMap.add (0, None) [ 0 ]
    }
  | Char c ->
    { states = [ 0; 1 ]
    ; start = 0
    ; accepting = [ 1 ]
    ; delta = TransMap.empty |> TransMap.add (0, Some c) [ 1 ]
    }
  | Union (re1, re2) -> union (re_to_nfa re1) (re_to_nfa re2)
;;

(* let test_word = List.of_seq (String.to_seq "aabbaa") *)
(**)
(* let test_fa = *)
(*   { states = [ 1; 2; 3 ] *)
(*   ; accepting = [ 3 ] *)
(*   ; start = 1 *)
(*   ; delta = *)
(*       TransMap.empty *)
(*       |> TransMap.add (1, Some 'a') 1 *)
(*       |> TransMap.add (1, Some 'b') 2 *)
(*       |> TransMap.add (2, Some 'a') 3 *)
(*       |> TransMap.add (2, Some 'b') 2 *)
(*       |> TransMap.add (3, Some 'a') 3 *)
(*   } *)
(* ;; *)
