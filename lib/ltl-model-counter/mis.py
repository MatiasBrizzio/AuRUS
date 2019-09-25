#!/usr/bin/env python3

# Copyright (C) 2016-2018   Kuldeep S. Meel
# Copyright (C) 2018        Mate Soos
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; version 2
# of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
# 02110-1301, USA.


from __future__ import with_statement  # Required in 2.5
from __future__ import print_function

import os
import time
import optparse


def parseOutput(fileName):
    f = open(fileName, 'r')
    lines = f.readlines()
    f.close()
    for line in lines:
        if (line.strip().startswith('v')):
            return line
    return ''


usage = "usage: %prog [options] <input.cnf>"
desc = """Gives the minimal independent set of the file.

If both --useind is set and --firstinds are specified, the union of both independent supports is considered

If --useind is set but there is no independent support in input file, and --firstinds is not specified, all variables are considered
"""


def set_up_parser():
    parser = optparse.OptionParser(usage=usage, description=desc)
    parser.add_option("--timeout", metavar="TOUT", dest="timeout", type=int,
                      default=3000, help="timeout for iteration in seconds (default: %default seconds)")
    parser.add_option("--verb,-v", default=1, type=int,
                      dest="verbosity", help="Higher numbers for more verbosity")
    parser.add_option("--noclean", action="store_true", default=False,
                      dest="noclean", help="Don't clean up temporary files")

    parser.add_option("--out", dest="outputfile", type=str, default=None,
                      help="Output file destination. Default is 'inputfile.ind'")
    parser.add_option("--log", dest="logfile", type=str, default="log.txt",
                      help="Log file destination. Deafult : %default")
    parser.add_option("--maxiter", dest="maxiter", type=int, default=1,
                      help="up to 'max' number of minimal independent supports will be generated. Default: %default")
    parser.add_option("--useind", dest="useind", action="store_true",
                      default=False,
                      help="use independent support provided in input file")
    parser.add_option("--glucose", action="store_true", default=False,
                      dest="glucose", help="Use glucose in muser2")
    parser.add_option("--bin", type=str, default="./muser2-dir/src/tools/muser2/muser2",
                      dest="bin", help="muser2 binary to use")


    return parser


if __name__ == "__main__":
    parser = set_up_parser()
    (options, args) = parser.parse_args()

    if len(args) < 1:
        print("ERROR: you must give a CNF input file as a parameter")
        exit(-1)

    inputfile = args[0]

    if options.outputfile is not None:
        outputfile = options.outputfile
    else:
        outputfile = inputfile + ".ind"

    mytime = time.time()
    if len(inputfile) > 4 and inputfile[-4:] == '.cnf':
        gmusFile = inputfile[:-4] + '.gcnf'
        tempOutFile = inputfile[:-4] + '.tcnf'
    else:
        gmusFile = outputfile + '.gcnf'
        tempOutFile = outputfile + '.tcnf'

    f = open(outputfile, 'w')
    f.close()

    cmd = "./togmus %s %s %s" % (inputfile, gmusFile, options.useind)
    print("Running togmus: '%s'" % cmd)
    os.system(cmd)
    print("togmus executed in %-3.2f" % (time.time()-mytime))

    # run maxiters iterations
    indMap = {}
    maxTry = 10
    attempts = 0
    for i in range(options.maxiter):
        mytime = time.time()
        if options.glucose:
            cmd = "%s -v 0 -grp -comp -glucose -order 4 -T %s %s > %s" % (
                    options.bin, options.timeout, gmusFile, tempOutFile)
        else:
            cmd = "%s -v 0 -grp -comp -minisats -order 4 -T %s %s > %s" % (
                    options.bin, options.timeout, gmusFile, tempOutFile)
        print("Running muser2: '%s'" % cmd)
        os.system(cmd)
        print("muser2 executed in %-3.2f" % (time.time()-mytime))

        indvars = parseOutput(tempOutFile)
        indvars = indvars.strip().lstrip(" v ")
        if indvars not in indMap:
            indMap[indvars] = 1
        else:
            attempts += 1
            if attempts >= maxTry:
                break
            else:
                continue

        mytime = time.time() - mytime
        if options.outputfile is not None:
            with open(outputfile, 'a') as f:
                f.write(indvars)

            if options.verbosity == 1:
                with open(options.logfile, 'a') as f:
                    f.write("%d:%d:%3.2f\n" % (i, i + attempts, mytime))
        else:
            print("num independent vars:", len(indvars.split())-1)
            print("** Copy-paste the following line in the top of your CNF for ApproxMC **")
            print("c ind %s" % indvars)

    if not options.noclean:
        os.unlink(tempOutFile)
        os.unlink(gmusFile)
