#!/usr/bin/env python3

import argparse
import os
from collections import OrderedDict

parser = argparse.ArgumentParser(description='tree parser')
parser.add_argument("-t", "--tree")
args = parser.parse_args()
file = args.tree

class Dependency:

    def __init__(self, group, artifact, type, version, scope):
        self._group = group
        self._artifact = artifact
        self._type = type
        self._version = version
        self._scope = scope
        self._dependencies = []


    def add_dep(self, dep):
        self._dependencies.append(dep)


    def getGroup(self):
        return self._group

    def getDeps(self):
        return self._dependencies

    def __str__(self):
        return self._group + ":" + self._artifact + ":" + self._version


def peekNextLine(file):
    pos = file.tell()
    line = file.readline()
    file.seek(pos)
    return line


def buildDep(file, parent):
    line = file.readline()

    if line[0] == " ":
        return
    else:
        level = line.index("-")
        child = parseDep(line[level + 2:])

        # Using this child as the lower level's parent build this child's deps
        nextLine = peekNextLine(file)
        while nextLine[0] != " " and nextLine.index("-") > level:
            buildDep(file, child)
            nextLine = peekNextLine(file)

        parent.add_dep(child)

def parseDep(raw):
    print("parsing: " + raw)
    raw = raw.split(":")
    group = raw[0]
    artifact = raw[1]
    type = raw[2]
    version = raw[3]
    if len(raw) == 5:
        scope = raw[4]
    else:
        scope = ""
    return Dependency(group, artifact, type, version, scope)


def topLevelDeps(root_deps):
    tlds = []
    for i in root_deps:
        if i.getGroup() == "org.locationtech.geomesa":
            tlds += topLevelDeps(i.getDeps())
        else:
            tlds.append(i)
    return tlds


def parseDeps(file):
    root_deps = []
    with open(file, 'r') as f:
        print("reading")
        line = f.readline()
        while line:
            if line.startswith("Building"):
                # parse deps for a module
                # need to skip warnings
                while peekNextLine(f)[:9] == "[WARNING]":
                    f.readline()
                # need to skip plugins
                if peekNextLine(f)[:3] == "---":
                    f.readline()
                root_dep = parseDep(f.readline())
                while peekNextLine(f)[0] != " ":
                    buildDep(f, root_dep)
                root_deps.append(root_dep)

            line = f.readline()
    return root_deps

deps = parseDeps(file)
tlds = topLevelDeps(deps)
tldsStrings = []

for i in tlds:
    tldsStrings.append(str(i))

print("Top Level Deps")
tldsStrings = list(set(tldsStrings))
for i in tldsStrings:
    print(i)

for i in deps:
