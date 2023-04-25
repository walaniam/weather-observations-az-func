#!/bin/bash

if [[ -z $1 ]]; then
  echo "Missing connection URL"
  exit 1
fi

if [[ -z $2 ]]; then
  echo "Missing API key"
  exit 1
fi

CONNECTION_URL="$1?code=$2"
INTERVAL_SECONDS=1

i=0
while true
do
  time=$(date '+%Y%m%d %H%M%S')
  # thank you stackoverflow :)
  out_temp=$(awk -v min=3 -v max=25 'BEGIN{srand(); print min+rand()*(max-min+1)}')
  in_temp=$(awk -v min=19 -v max=23 'BEGIN{srand(); print min+rand()*(max-min+1)}')
  pressure=$(awk -v min=970 -v max=999 'BEGIN{srand(); print min+rand()*(max-min+1)}')

  every_10th=$(expr $i % 10)
  every_21st=$(expr $i % 21)

  if [[ $every_10th -eq 0 ]]; then
    observation="not_a_date,${out_temp},${in_temp},${pressure}"
  elif [[ $every_21st -eq 0 ]]; then
    observation=""
  else
    observation="${time},${out_temp},${in_temp},${pressure}"
  fi

  echo "Sending: $observation"
  curl -i -d "$observation" -X POST "$CONNECTION_URL"

  sleep $INTERVAL_SECONDS

  ((i=i+1))
done