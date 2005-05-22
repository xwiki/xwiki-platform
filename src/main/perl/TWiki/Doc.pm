

package TWiki::Doc;

use strict;

sub new
{
 my $self = {};
 bless $self;
 return $self;
}

sub getWeb
{
 return "Main";
}

sub getName
{
 return "WebHome";
}

1;
