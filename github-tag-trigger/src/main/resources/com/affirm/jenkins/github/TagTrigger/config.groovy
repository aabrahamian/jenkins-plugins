j = namespace("jelly:core")
f = namespace("/lib/form")

f.entry(field: "regex", title:_("Pattern")) {
    f.textbox(default: ".*")
}