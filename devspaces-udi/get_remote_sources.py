import yaml  # TODO: Cleanup these imports so we only import functions we need 
import sys
import os
from git import Repo  # pip install gitpython

# TODO: Clarify this description
# Related to container.yaml file
CONTAINER_YAML="container.yaml"
REMOTE_SOURCES_KEY="remote_sources"

# For individual remote_source objects in container.yaml
NAME_KEY="name"
REMOTE_SOURCE_KEY="remote_source"
REPO_KEY="repo"
REF_KEY="ref"


# Directory containing cloned remote sources
# TODO: Rename?
PATH="remote_sources"
APP_PATH="app"


class ParsedSource:
    def __init__(self, name, repo_url, commit_hash):
        self.name = name
        self.repo_url = repo_url
        self.commit_hash = commit_hash

    def getName(self):
        return self.name
    
    def getRepoURL(self):
        return self.repo_url
    
    def getCommitHash(self):
        return self.commit_hash


def assert_key_exists(key, dict):
    if key not in dict:
        missing_key_error(key, dict)

def missing_key_error(key, dict):
    sys.exit("Expected '{}' in '{}' but was not present.".format(key, dict))




parsed_remote_sources = []

print("Reading {}".format(CONTAINER_YAML))

with open(CONTAINER_YAML, 'r') as file:
    container_yaml_parsed = yaml.safe_load(file)
    #print(container_yaml_parsed)
    assert_key_exists(REMOTE_SOURCES_KEY, container_yaml_parsed)

    remote_sources_list = container_yaml_parsed[REMOTE_SOURCES_KEY]
    #print("Remote sources list: " + str(remote_sources_list))
    for remote_source in remote_sources_list:

        # Validate remote source
        assert_key_exists(NAME_KEY, remote_source)
        assert_key_exists(REMOTE_SOURCE_KEY, remote_source)

        name = remote_source[NAME_KEY]
        # TODO: Rename remote_source and git_source, kind of confusing
        git_source = remote_source[REMOTE_SOURCE_KEY]
        print("Parsing remote source '{}'".format(name))


        # Validate git source
        assert_key_exists(REPO_KEY, git_source)
        assert_key_exists(REF_KEY, git_source)

        repo_url = git_source[REPO_KEY]
        commit_hash = git_source[REF_KEY]
        parsed_source = ParsedSource(name, repo_url, commit_hash)

        parsed_remote_sources.append(parsed_source)


if len(parsed_remote_sources) > 0:
    os.getcwd()
    parent_directory_path = os.path.join(os.getcwd(), PATH)
    try:
        os.mkdir(parent_directory_path)  
        print("Created directory: {}".format(parent_directory_path))
    except OSError as error:  
        sys.exit("Unable to create directory {}: {}".format(parent_directory_path, repr(error)))
  
    for remote_source in parsed_remote_sources:

        repo_name = remote_source.getName()
        repo_url = remote_source.getRepoURL()
        ref = remote_source.getCommitHash()


        if repo_name == "python-deps":
            # TODO: Need to implement cloning only a subdirectory, I guess with git sparse checkout
            continue

        repo_path = os.path.join(parent_directory_path, repo_name)
        # TODO: Check for errors:
        Repo.clone_from(repo_url, repo_path)
        print("Successfully cloned '{}'".format(repo_name))


        # TODO: error checking
        cloned_repo = Repo(repo_path)
        # TODO: Find better name for head instead of specified_version
        ref_to_checkout = cloned_repo.create_head("specified_version", ref)
        cloned_repo.head.reference = ref_to_checkout
        cloned_repo.head.reset(index=True, working_tree=True)

        print("Successfully checked out ref {} for repo '{}'".format(ref, repo_name))

else:
    sys.exit("No remote sources were parsed. Nothing to do.")
    