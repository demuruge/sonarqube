import React from 'react';

export default React.createClass({
  render() {
    const url = `${baseUrl}/profiles/show?key=${encodeURIComponent(this.props.profile)}`;
    return <a href={url}>{this.props.children}</a>;
  }
});
