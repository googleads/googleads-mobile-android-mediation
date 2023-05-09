include "devtools/blueprint/ncl/blueprint_file.ncl";
include "releasetools/rapid/ncl/rapid_config.ncl";

// Library for constructing NCL format Rapid configurations.
namespace Rapid {

def BlueprintFile(
    // adapter folder name, case-sensetive
    adapter_folder_name : string) =
  ::blueprint::BlueprintFile(
    // Blueprint files with the same project_name field, can be viewed
    // in the Rapid UI by going to: http://rapid/#/projectGroupFamily/<project_name>
    // This will allow us to see all the android adapter release projects
    // together under same "project family".
    project_name = "gma-sdk-android-adapter",
    tech_lead = ["tukn", "thanvir"],
    mdb_groups = ["mobile-ads-sdk-ext"],
    buganizer_component_ids = [1359673],
    dev_mailing_list = "gma-adapter-builds@google.com",
    teams_product_id = 2132879172713,
    owned_code_depotpaths = [
      "//depot/google3/third_party/java_src/gma_sdk_mediation/third_party_adapters/%s/..." % ::string::to_lower(adapter_folder_name),
    ],
    releasable_units = [
      ::blueprint::ReleasableUnit(
        // Rapid project name
        name = "gma-sdk-android-%s-adapter" % ::string::to_lower(adapter_folder_name),
        rapid_config = ::Rapid::RapidConfig(
          default_create_candidate_workflow = ::Rapid::Workflow::CreateCandidate(
            name = "Create Candidate",
            config_path = "google3/third_party/java_src/gma_sdk_mediation/rapid/create_adapter_candidate.pp",
            parameters = [
              ::Rapid::WorkflowParameter::Simple(
                name = "adapter_name",
                description = "Name of adapter to build.",
                default_value = ::string::to_lower(adapter_folder_name),
                modifiable = false,
              ),
            ],
          ),
          grant_settings = ::Rapid::GrantSettings(
            grant = [
              ::Rapid::Grant::Group("mobile-ads-sdk", ::Rapid::Role::ReleaseManager),
              ::Rapid::Grant::User("tukn", ::Rapid::Role::Owner),
              ::Rapid::Grant::User("imansi", ::Rapid::Role::Owner),
            ],
          ),
          runner_config = ::Rapid::RunnerConfig(
            runner_user = "mobile-ads-sdk-releaser",
            charged_user = "ads-display-releaser-accounting",
          ),
        ),
      ),
    ],
  );

}
