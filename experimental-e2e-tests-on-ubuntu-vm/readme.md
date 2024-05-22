The purpose of this effort is to simulate the behavior of CI pipeline which performs end to end tests on workflows when we raise PR. Thus instead of checking if the workflow end to end tests are passing on github, we can use a simple Ubuntu VM and run scripts to see if the tests are passing.

## Prerequisites
- An available Ubuntu Server 24.04 LTS VM with specs equivalent to AWS' t3.2xlarge specifications

## Testing mta-v6.x workflow
- Clone the serverless-workflows repo which includes the code to ci tests
```shell
ssh <your ubuntu vm>
git clone https://github.com/rhkp/serverless-workflows.git -b flpath751
cd serverless-workflows/experimental-e2e-tests-on-ubuntu-vm/
chmod +x *.sh

export REGISTRY_REPO=<Your Qauy.io registry repo>
```

- Setup the ubuntu vm with required prerequisite software components
```shell
./ubuntuvm.sh
```

- Test the mta-v6.x workflow
```shell
export WORKFLOW_ID=mta-v6.x
./mta-v6.sh
```

- Test the mta-v7.x workflow
```shell
export WORKFLOW_ID=mta-v7.x
./mta-v7.sh
```