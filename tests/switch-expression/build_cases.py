"""Build gold traces from explicit grammar derivations, without parsing compiler output.

The supplied sample is copied verbatim from docs/SWITCH_EXPRESSION_PROBLEM.txt.
Other correct cases describe production trees; terminals form the input and
postorder production names form the expected parser trace. Error answers are
handwritten from the missing token's preceding terminal and line number.
"""
from dataclasses import dataclass
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent.parent


@dataclass
class Leaf:
    kind: str
    text: str

    def tokens(self):
        return [self]

    def trace(self):
        return [f"{self.kind} {self.text}"]


@dataclass
class Node:
    tag: str
    children: tuple

    def tokens(self):
        return [token for child in self.children for token in child.tokens()]

    def trace(self):
        lines = [line for child in self.children for line in child.trace()]
        return lines if self.tag in {"Decl", "BlockItem", "BType"} else lines + [f"<{self.tag}>"]


def n(tag, *children): return Node(tag, children)
KINDS = dict(zip(
    "int char void main const static if else while switch case default break continue return printf + - * / % ; , : -> ( ) [ ] { } = < > == && || !".split(),
    "INTTK CHARTK VOIDTK MAINTK CONSTTK STATICTK IFTK ELSETK WHILETK SWITCHTK CASETK DEFAULTTK BREAKTK CONTINUETK RETURNTK PRINTFTK PLUS MINU MULT DIV MOD SEMICN COMMA COLON ARROW LPARENT RPARENT LBRACK RBRACK LBRACE RBRACE ASSIGN LSS GRE EQL AND OR NOT".split()))
def t(text): return Leaf(KINDS[text], text)
def ident(name): return Leaf("IDENFR", name)
def number(value): return n("Number", Leaf("CHARCON" if str(value).startswith("'") else "INTCON", str(value)))
def wrapped(primary): return n("Exp", n("AddExp", n("MulExp", n("UnaryExp", n("PrimaryExp", primary)))))
def num(value): return wrapped(number(value))
def lval(name, index=None): return n("LVal", ident(name), *([] if index is None else [t("["), index, t("]")]))
def var(name, index=None): return wrapped(lval(name, index))
def unary_of(e):
    add = e.children[0]
    if len(add.children) == 1 and len(add.children[0].children) == 1:
        return add.children[0].children[0]
    return n("UnaryExp", n("PrimaryExp", t("("), e, t(")")))
def mul_of(e):
    add = e.children[0]
    return add.children[0] if len(add.children) == 1 else n("MulExp", unary_of(e))
def unary(op, e): return n("Exp", n("AddExp", n("MulExp", n("UnaryExp", n("UnaryOp", t(op)), unary_of(e)))))
def cast(kind, e): return n("Exp", n("AddExp", n("MulExp", n("UnaryExp", t("("), n("BType", t(kind)), t(")"), unary_of(e)))))
def binary(op, left, right):
    if op in {"+", "-"}:
        return n("Exp", n("AddExp", left.children[0], t(op), mul_of(right)))
    return n("Exp", n("AddExp", n("MulExp", mul_of(left), t(op), unary_of(right))))
def separated(values):
    return tuple(item for i, value in enumerate(values) for item in ([t(","), value] if i else [value]))
def call(name, *args):
    params = [n("FuncRParams", *separated(args))] if args else []
    return n("Exp", n("AddExp", n("MulExp", n("UnaryExp", ident(name), t("("), *params, t(")")))))
def arm(label, e): return n("SwitchArm", *([t("default")] if label is None else [t("case"), number(label)]), t("->"), e, t(";"))
def switch(selector, *arms): return wrapped(n("SwitchExp", t("switch"), t("("), selector, t(")"), t("{"), *arms, t("}")))
def block(*items): return n("Block", t("{"), *items, t("}"))
def main(*items): return n("MainFuncDef", t("int"), t("main"), t("("), t(")"), block(*items))
def unit(*items): return n("CompUnit", *items)
def ret(e=None): return n("Stmt", t("return"), *([] if e is None else [e]), t(";"))
def statement(e): return n("Stmt", e, t(";"))
def assign(name, e, index=None): return n("Stmt", lval(name, index), t("="), e, t(";"))
def decl(name, value=None, kind="int", length=None, static=False, const=False):
    tail = [] if length is None else [t("["), n("ConstExp", length.children[0]), t("]")]
    if value is not None:
        tail += [t("="), n("ConstInitVal" if const else "InitVal", n("ConstExp", value.children[0]) if const else value)]
    definition = n("ConstDef" if const else "VarDef", ident(name), *tail)
    prefix = [t("const")] if const else ([t("static")] if static else [])
    return n("ConstDecl" if const else "VarDecl", *prefix, n("BType", t(kind)), definition, t(";"))
def cond(e, op=None, right=None):
    rel = n("RelExp", e.children[0]) if op is None else n("RelExp", n("RelExp", e.children[0]), t(op), right.children[0])
    return n("Cond", n("LOrExp", n("LAndExp", n("EqExp", rel))))
def if_stmt(c, yes, no=None): return n("Stmt", t("if"), t("("), c, t(")"), yes, *([] if no is None else [t("else"), no]))
def while_stmt(c, body): return n("Stmt", t("while"), t("("), c, t(")"), body)
def case(label, *stmts): return n("CaseStmt", *([t("default")] if label is None else [t("case"), number(label)]), t(":"), *stmts)
def switch_stmt(e, *cases): return n("Stmt", t("switch"), t("("), e, t(")"), t("{"), *cases, t("}"))
def keyword_stmt(word): return n("Stmt", t(word), t(";"))
def printf(fmt, *args): return n("Stmt", t("printf"), t("("), Leaf("STRCON", fmt), *sum(([t(","), e] for e in args), []), t(")"), t(";"))
def func(name, body, kind="int", params=()):
    formal = [n("FuncFParams", *separated(n("FuncFParam", n("BType", t(k)), ident(v)) for k, v in params))] if params else []
    return n("FuncDef", n("FuncType", t(kind)), ident(name), t("("), *formal, t(")"), body)


manifest = []
def correct(case_id, title, tree, source=None):
    path = ROOT / "cases" / case_id
    path.mkdir(parents=True, exist_ok=True)
    if source is None:
        source = " ".join(token.text + ("\n" if token.text in {";", "{", "}"} else "") for token in tree.tokens())
    (path / "testfile.txt").write_text(source.rstrip() + "\n", encoding="utf-8")
    (path / "parser.txt").write_text("\n".join(tree.trace()) + "\n", encoding="utf-8")
    (path / "lexer.txt").write_text("\n".join(token.trace()[0] for token in tree.tokens()) + "\n", encoding="utf-8")
    (path / "error.txt").write_text("", encoding="utf-8")
    manifest.append(dict(id=case_id, title=title, correct=True))
def error(case_id, title, source, answer):
    path = ROOT / "cases" / case_id
    path.mkdir(parents=True, exist_ok=True)
    (path / "testfile.txt").write_text(source, encoding="utf-8")
    (path / "error.txt").write_text(answer, encoding="utf-8")
    manifest.append(dict(id=case_id, title=title, correct=False))


correct("testcase1", "unchanged: declarations and arithmetic", unit(main(decl("x", num(2)), ret(binary("+", var("x"), num(3))))))
loop = while_stmt(cond(var("i"), "<", num(2)), n("Stmt", block(assign("i", binary("+", var("i"), num(1))))))
old_switch = switch_stmt(var("c"), case("'a'", printf('"%c ->\\n"', call("echo", var("c"))), keyword_stmt("break")), case(None, n("Stmt", t(";"))))
correct("testcase2", "unchanged: functions, char, while and statement switch", unit(
    func("echo", block(ret(var("c"))), kind="char", params=(("char", "c"),)),
    main(decl("c", num("'a'"), kind="char", static=True), decl("i", num(0)), loop, old_switch, ret(num(0)))))
correct("testcase3", "syntax only: no semantic error codes", unit(main(
    keyword_stmt("break"), keyword_stmt("continue"), assign("unknown", call("undefined", num(1))))))
error("testcase4", "unchanged: i/j/k recovery", "int main() {\n    int a[2;\n    f(1;\n    return 0\n}\n", "2 k\n3 j\n4 i\n")
error("testcase5", "unchanged: illegal logical operator", "int main() {\n    if (1 & 1) return 0;\n    return 0;\n}\n", "2 a\n")

# The supplied correct example and its trace are an independent, literal oracle.
problem = (REPO / "docs/SWITCH_EXPRESSION_PROBLEM.txt").read_text(encoding="utf-8")
sample = problem.split("正确样例\n", 1)[1].split("parser.txt：", 1)[0].strip()
sample_trace = problem.split("parser.txt：", 1)[1].split("错误样例", 1)[0].strip()
sample_tree = unit(main(decl("x", num(2)), switch_stmt(var("x"), case(2, assign("x", num(3)))),
                        decl("y", switch(var("x"), arm(3, num(30)), arm(None, num(0)))), ret(var("y"))))
assert sample_tree.trace() == sample_trace.splitlines(), "Grammar oracle disagrees with the supplied sample"
correct("testcase6", "supplied sample: both switch positions", sample_tree, sample)

s = switch(num(1), arm(1, unary("-", num(2))), arm(None, unary("+", num(3))))
correct("testcase7", "arithmetic, casts, arguments and adjacent arrow/minus", unit(main(
    decl("y", binary("+", binary("*", num(2), s), num(4))),
    statement(call("f", cast("char", s), binary("%", s, num(3)))), ret(var("y")))))
compact_source = ROOT / "cases/testcase7/testfile.txt"
compact_source.write_text(compact_source.read_text(encoding="utf-8").replace("-> - ", "->-").replace("-> + ", "->+"), encoding="utf-8")
inner = switch(var("x"), arm(1, num(2)), arm(None, num(3)))
nested = switch(inner, arm(None, switch(num(0))), arm(2, inner), arm(3, num(4)))
correct("testcase8", "nested selectors and arm values", unit(main(decl("x", num(1)), ret(nested))))
index = switch(var("x"), arm(0, num(1)), arm(None, num(2)))
correct("testcase9", "array index assignment across inner semicolons", unit(main(
    decl("a", length=num(4)), assign("a", num(7), index), statement(var("a", index)), ret(var("a", index)))))
parenthesized = n("Exp", n("AddExp", n("MulExp", n("UnaryExp", n("PrimaryExp", t("("), inner, t(")"))))))
correct("testcase10", "constant/static/condition/parenthesized statement contexts", unit(
    decl("L", switch(num(1), arm(None, num(3))), const=True),
    main(decl("a", length=switch(num(0), arm(None, num(2)))), decl("v", inner, static=True),
         if_stmt(cond(inner, "<", num(4)), statement(call("f", inner))), statement(parenthesized), ret(inner))))
error("testcase11", "supplied missing-arrow sample", "int main() {\n    int x = 2;\n    int y = switch (x) {\n        case 3 30;\n        default -> 0;\n    };\n    return 0;\n}\n", "4 o\n")
error("testcase12", "missing semicolon after multiline arm value", "int main() {\n    int y = switch (1) {\n        case 1 ->\n            10 +\n            2\n        default -> 0;\n    };\n    return 0;\n}\n", "5 i\n")
error("testcase13", "missing selector right parenthesis", "int main() {\n    return switch (\n        1 + 2\n    {\n        default -> 0;\n    };\n}\n", "3 j\n")
error("testcase14", "nested recovery, default separator and same-line errors", "int main() {\n    int x = switch (\n        1\n    ) {\n        case 1\n            switch (2 {\n                case 2 20\n                default\n                    0;\n            }\n        default -> 3;\n    };\n    return 0;\n}\n", "5 o\n6 j\n7 i\n7 o\n8 o\n10 i\n")
value_switch = switch(var("x"), arm(0, call("choose", num(1))), arm(None, num(0)))
integrated_switch = switch_stmt(inner, case(2, assign("a", value_switch, index), keyword_stmt("break")), case(None, n("Stmt", t(";"))))
integrated_loop = while_stmt(cond(var("x"), "<", num(3)), n("Stmt", block(integrated_switch, assign("x", binary("+", var("x"), num(1))))))
correct("testcase15", "integrated statement/expression switches and array assignment", unit(
    func("choose", block(ret(inner)), params=(("int", "x"),)),
    main(decl("x", num(0)), decl("a", length=num(5)), integrated_loop,
         printf('"%d %c ->\\n"', inner, cast("char", num(65))), ret(inner))))

# Supplementary boundaries beyond the stated 15 test categories.
duplicates = switch(num(1), arm(1, num(1)), arm(1, num(2)), arm(None, num(3)), arm(None, num(4)))
correct("empty_and_duplicate_arms", "zero arms and repeated labels are syntactically valid", unit(main(decl("x", switch(num(0))), ret(duplicates))))
deep = num(1)
for _ in range(12):
    deep = switch(num(0), arm(None, deep))
correct("deep_nesting", "twelve nested switch expressions", unit(main(ret(deep))))
error("multiline_character_arrow", "character/default labels retain their own source lines", "int main() {\n    return switch ('a') {\n        case '\\n'\n            1;\n        default\n            0;\n    };\n}\n", "3 o\n5 o\n")
error("missing_index_bracket", "assignment lookahead preserves k recovery", "int main() {\n    a[switch (1) { default -> 0; } = 2;\n    return 0;\n}\n", "2 k\n")
crlf_dir = ROOT / "cases/crlf_sample"
correct("crlf_sample", "CRLF source matches the supplied trace", sample_tree, sample)
(crlf_dir / "testfile.txt").write_bytes((sample + "\n").replace("\n", "\r\n").encode("utf-8"))
(ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Built {len(manifest)} cases from the problem statement and explicit grammar derivations.")
