import json
import yaml
import argparse


def load_openapi_spec(file_path):
    """Load the OpenAPI specification from a JSON of YAML file."""

    with open(file_path, 'r') as file:        
        if file_path.lower().endswith(('.yml', '.yaml')):
            return json.loads(json.dumps(yaml.safe_load(file)))

        elif file_path.lower().endswith('.json'):            
            return json.load(file)
        
        else: 
            raise ValueError("error: input file is not json or yaml")


def extract_schemas(
        schema_ref, openapi_spec, collected_schemas, visited_refs=None):
    """Recursively extract schemas referenced in the OpenAPI spec."""
    if visited_refs is None:
        visited_refs = set()

    schema_name = schema_ref.split('/')[-1]

    if schema_name not in visited_refs:
        visited_refs.add(schema_name)

        schema_definition = openapi_spec.get(
            'definitions', {}).get(schema_name) or openapi_spec.get(
                'components', {}).get('schemas', {}).get(schema_name)

        if schema_definition:
            collected_schemas[schema_name] = schema_definition

            for key, value in schema_definition.items():
                if isinstance(value, dict):
                    if '$ref' in value:
                        extract_schemas(value['$ref'], openapi_spec,
                                        collected_schemas, visited_refs)
                    elif key in {'properties', 'items'}:
                        extract_schemas_from_dict(
                            value, openapi_spec, collected_schemas, visited_refs)
                elif isinstance(value, list):
                    for item in value:
                        if isinstance(item, dict) and '$ref' in item:
                            extract_schemas(item['$ref'], openapi_spec,
                                         collected_schemas, visited_refs)
                        elif isinstance(item, dict):
                            extract_schemas_from_dict(
                                item, openapi_spec, collected_schemas, visited_refs)


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
    if 'parameters' not in openapi_spec and 'components' not in openapi_spec:
        return

    parameter = openapi_spec.get('parameters', {}).get(spec_ref) or \
                openapi_spec.get('components', {}).get('parameters', {}).get(spec_ref)

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
    operation_spec = {
        "paths": {},
        "components": {
            "schemas": {},
            "parameters": {}
        }
    }

    operation = openapi_spec['paths'].get(path, {}).get(method.lower(), {})

    if operation:
        operation_spec["paths"][path] = {method.lower(): operation}

        parameters = operation.get('parameters', [])
        collected_schemas = {}
        extract_parameters(parameters, openapi_spec, collected_schemas, visited_refs=set())

        request_body = operation.get('requestBody', {})
        if 'content' in request_body:
            for content_type, content_value in request_body['content'].items():
                schema_ref = content_value.get('schema', {}).get('$ref', '')
                if schema_ref:
                    extract_schemas(schema_ref, openapi_spec, collected_schemas)

        operation_spec["components"]["schemas"] = collected_schemas

        for param in parameters:
            if '$ref' in param:
                param_ref = param['$ref'].split('/')[-1]
                operation_spec["components"]["parameters"][param_ref] = openapi_spec.get('components', {}).get(
                    'parameters', {}).get(param_ref, {})

    else:
        print(f"Could not find the {method.upper()} operation for path {path}.")

    return operation_spec


def merge_specs(base_spec, new_spec):
    """Merge the new operation spec into the base spec."""
    for path, methods in new_spec.get("paths", {}).items():
        if path not in base_spec["paths"]:
            base_spec["paths"][path] = {}
        base_spec["paths"][path].update(methods)

    for schema_name, schema in new_spec.get("components", {}).get("schemas", {}).items():
        base_spec["components"]["schemas"].setdefault(schema_name, schema)

    for parameter_name, parameter in new_spec.get("components", {}).get("parameters", {}).items():
        base_spec["components"]["parameters"].setdefault(parameter_name, parameter)


def add_security_scheme(openapi_spec):
    """Add a BearerToken security scheme to the OpenAPI spec."""
    security_scheme = {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT"
    }

    openapi_spec.setdefault("components", {}).setdefault("securitySchemes", {})
    openapi_spec["components"]["securitySchemes"]["BearerToken"] = security_scheme


def save_openapi_spec(file_path, openapi_spec):
    """Save the new OpenAPI specification to a YAML file."""
    with open(file_path, 'w') as file:
        yaml.dump(openapi_spec, file, sort_keys=False)


def main():
    parser = argparse.ArgumentParser(description='Extract specific operations and their schemas from an OpenAPI spec.')
    parser.add_argument('input_file', help='Path to the input OpenAPI spec JSON or YAML file.')
    parser.add_argument('output_file', help='Path to the output YAML file for the filtered OpenAPI spec.')
    parser.add_argument('extractions', nargs='+',
                        help='Tuples representing (path, method) to extract, e.g., "/apis/apps/v1/namespaces/{namespace}/deployments post"')

    args = parser.parse_args()

    openapi_spec = load_openapi_spec(args.input_file)

    new_openapi_spec = {
        "openapi": openapi_spec.get("openapi", "3.0.0"),
        "info": openapi_spec.get("info", {}),
        "paths": {},
        "components": {
            "schemas": {},
            "parameters": {}
        }
    }

    for extraction in args.extractions:
        path, method = extraction.split(maxsplit=1)
        operation_spec = extract_operation_and_definitions(openapi_spec, path, method)
        merge_specs(new_openapi_spec, operation_spec)

    add_security_scheme(new_openapi_spec)

    save_openapi_spec(args.output_file, new_openapi_spec)

    print(f"Filtered OpenAPI spec with BearerToken security scheme saved to {args.output_file}")


if __name__ == "__main__":
    main()