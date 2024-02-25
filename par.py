#!/usr/bin/env python3
'''
PAR -- Processes Activity Recorder
Utility to record all processes activity in time-series db.
Just like ps or top utility with hystory.
'''

import psutil
import json
import time
import os

_hours = 1
_minutes = 1
_seconds = 10
cycle = _seconds * _minutes * _hours
i = 0

with open('data.json', 'w') as f:
  f.write('[')
  while i < cycle:
    ts = time.time()
    f.write('{ "_timestamp": "' + str(ts) + '",\n')
    f.write('"_processes": [\n')
    print(f"Slicing processes on {time.asctime()}")
    _comma = False
    for p in psutil.process_iter(['pid', 'ppid', 'name', 'memory_info', 'memory_percent', 'cpu_times', 'exe']):
    #for p in psutil.process_iter(['ppid', 'memory_info', 'memory_percent', 'cpu_times', 'exe']):
      try:
        #dict_proc = p.as_dict()
        dict_proc = p.info
        if _comma:
          f.write(',\n')
        json.dump(dict_proc, f)
        _comma = True
      except psutil.NoSuchProcess as err:
        print(err)
    i += 1
    f.write(']}')
    if i < cycle: f.write(',')
    print(f"\t\tdone for {time.time() - ts} sec.")
    time.sleep(1)
  f.write(']')
