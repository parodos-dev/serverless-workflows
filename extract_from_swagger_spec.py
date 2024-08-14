import json
import argparse

def load_openapi_spec(file_path):
    """Load the OpenAPI specification from a JSON file."""
    with open(file_path, 'r') as file:
        return json.load(file)

def extract_schemas(schema_ref, openapi_spec, collected_schemas, visited_refs=None):
    """Recursively extract schemas referenced in the OpenAPI spec."""
    if visited_refs is None:
        visited_refs = set()

    schema_name = schema_ref.split('/')[-1]

    if schema_name not in visited_refs:
        visited_refs.add(schema_name)
        schema_definition = openapi_spec['definitions'].get(schema_name)
        if schema_definition:
            collected_schemas[schema_name] = schema_definition

            # Recursively look for other $ref in the schema definition
            for key, value in schema_definition.items():
                if isinstance(value, dict):
                    if '$ref' in value:
                        extract_schemas(value['$ref'], openapi_spec, collected_schemas, visited_refs)
                    elif key in {'properties', 'items'}:
                        # Recursively handle properties and items
                        extract_schemas_from_dict(value, openapi_spec, collected_schemas, visited_refs)
                elif isinstance(value, list):
                    for item in value:
                        if isinstance(item, dict) and '$ref' in item:
                            extract_schemas(item['$ref'], openapi_spec, collected_schemas, visited_refs)
                        elif isinstance(item, dict):
                            extract_schemas_from_dict(item, openapi_spec, collected_schemas, visited_refs)

def extract_schemas_from_dict(schema_dict, openapi_spec, collected_schemas, visited_refs):
    """Extract schemas from a dictionary, including deeply nested dictionaries and lists."""
    if isinstance(schema_dict, dict):
        for key, value in schema_dict.items():
            if isinstance(value, dict):
                if '$ref' in value:
                    extract_schemas(value['$ref'], openapi_spec, collected_schemas, visited_refs)
                else:
                    extract_schemas_from_dict(value, openapi_spec, collected_schemas, visited_refs)
            elif isinstance(value, list):
                for item in value:
                    if isinstance(item, dict):
                        if '$ref' in item:
                            extract_schemas(item['$ref'], openapi_spec, collected_schemas, visited_refs)
                        else:
                            extract_schemas_from_dict(item, openapi_spec, collected_schemas, visited_refs)

def extract_parameters_from_spec(spec_ref, openapi_spec, collected_schemas, visited_refs):
    """Extract schemas referenced in parameters and add to collected_schemas."""
    parameter = openapi_spec['parameters'].get(spec_ref)
    if parameter:
        if 'schema' in parameter and '$ref' in parameter['schema']:
            schema_ref = parameter['schema']['$ref']
            extract_schemas(schema_ref, openapi_spec, collected_schemas, visited_refs)

def extract_parameters(parameters, openapi_spec, collected_schemas, visited_refs):
    """Extract schemas referenced in parameters."""
    for param in parameters:
        if 'schema' in param and '$ref' in param['schema']:
            schema_ref = param['schema']['$ref']
            extract_schemas(schema_ref, openapi_spec, collected_schemas, visited_refs)
        elif '$ref' in param:
            extract_parameters_from_spec(param['$ref'].split('/')[-1], openapi_spec, collected_schemas, visited_refs)

def extract_operation_and_definitions(openapi_spec, path, method):
    """Extract the specified operation and its associated schemas."""
    # Initialize the new spec structure for this operation
    operation_spec = {
        "paths": {},
        "definitions": {}
    }

    # Extract the specified operation
    operation = openapi_spec['paths'].get(path, {}).get(method.lower(), {})

    if operation:
        # Add the operation to the operation spec's paths
        operation_spec["paths"][path] = {method.lower(): operation}

        # Extract schemas referenced in parameters
        parameters = operation.get('parameters', [])
        collected_schemas = {}
        extract_parameters(parameters, openapi_spec, collected_schemas, visited_refs=set())

        # Extract schemas referenced in requestBody if it exists
        request_body = operation.get('requestBody', {})
        if 'content' in request_body:
            for content_type, content_value in request_body['content'].items():
                schema_ref = content_value.get('schema', {}).get('$ref', '')
                if schema_ref:
                    extract_schemas(schema_ref, openapi_spec, collected_schemas)

        # Add collected schemas to the operation spec
        operation_spec["definitions"] = collected_schemas
    else:
        print(f"Could not find the {method.upper()} operation for path {path}.")

    return operation_spec

def merge_specs(base_spec, new_spec):
    """Merge the new operation spec into the base spec."""
    # Merge paths
    for path, methods in new_spec.get("paths", {}).items():
        if path not in base_spec["paths"]:
            base_spec["paths"][path] = {}
        base_spec["paths"][path].update(methods)

    # Merge definitions
    for definition_name, definition in new_spec.get("definitions", {}).items():
        base_spec["definitions"].setdefault(definition_name, definition)

def save_openapi_spec(file_path, openapi_spec):
    """Save the new OpenAPI specification to a JSON file."""
    with open(file_path, 'w') as file:
        json.dump(openapi_spec, file, indent=4)

def main():
    # Parse command-line arguments
    parser = argparse.ArgumentParser(description='Extract specific operations and their schemas from an OpenAPI spec.')
    parser.add_argument('input_file', help='Path to the input OpenAPI spec JSON file.')
    parser.add_argument('output_file', help='Path to the output JSON file for the filtered OpenAPI spec.')
    parser.add_argument('extractions', nargs='+', help='Tuples representing (path, method) to extract, e.g., "/apis/apps/v1/namespaces/{namespace}/deployments post"')

    args = parser.parse_args()

    # Load the OpenAPI spec from the provided JSON file
    openapi_spec = load_openapi_spec(args.input_file)

    # Initialize the new OpenAPI spec
    new_openapi_spec = {
        "swagger": openapi_spec.get("swagger", "2.0"),
        "info": openapi_spec.get("info", {}),
        "paths": {},
        "definitions": {},
        "parameters": openapi_spec.get("parameters", {})
    }

    # Extract each specified operation and merge into the new spec
    for extraction in args.extractions:
        path, method = extraction.split(maxsplit=1)
        operation_spec = extract_operation_and_definitions(openapi_spec, path, method)
        merge_specs(new_openapi_spec, operation_spec)

    # Save the new OpenAPI spec to a file
    save_openapi_spec(args.output_file, new_openapi_spec)

    print(f"Filtered OpenAPI spec saved to {args.output_file}")

if __name__ == "__main__":
    main()
# python extract_from_swagger_spec.py openapi_spec.json filtered_openapi_spec.json "/apis/apps/v1/namespaces/{namespace}/deployments post" "/apis/apps/v1/namespaces/{namespace}/deployments/{name} get" "/api/v1/namespaces/{namespace}/services post" "/apis/route.openshift.io/v1/namespaces/{namespace}/routes post" "/apis/route.openshift.io/v1/namespaces/{namespace}/routes/{name} get" "/apis/route.openshift.io/v1/namespaces/{namespace}/routes/{name}/status get"
