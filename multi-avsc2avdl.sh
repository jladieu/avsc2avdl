#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo Accepts an .avsc file with multiple schemata, then outputs an individual .avdl file for each schema.
    echo
    echo "Usage: $0 <input_json_path> <output_directory>"
    exit 1
fi

INPUT_JSON="$1"
OUTPUT_DIR="$2"

# Ensure the output directory exists
mkdir -p "$OUTPUT_DIR"

# Process each schema in the input JSON
jq -c --monochrome-output '.[]' "$INPUT_JSON" | while read -r line; do
    filename=$(echo "$line" | jq -r '.name')
    output_avsc="$OUTPUT_DIR/${filename}.avsc"
    output_avdl="$OUTPUT_DIR/${filename}.avdl"

    echo "$line" > "$output_avsc"

    # Run the command and save the output to a file with .avdl extension
    # Note: we pipe to dev/null so that gradle doesn't consume the input,
    # for more information about this, see:
    # https://stackoverflow.com/questions/60499226/gradle-breaks-out-of-bash-for-loop
    ./gradlew run --args "$output_avsc $output_avdl" </dev/null
done

# Cleanup: Remove temporary .avsc files
rm -f "$OUTPUT_DIR"/*.avsc
